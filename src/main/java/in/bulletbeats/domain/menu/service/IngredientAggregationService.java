package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.PreparedItem;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.inventory.service.PreparedItemService;
import in.bulletbeats.domain.inventory.service.UnitService;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.entity.RecipeLine;
import in.bulletbeats.domain.shared.enums.MovementType;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves how much grocery-item and prepared-item stock a menu item's recipe consumes, and
 * applies that as stock movements. Shared by every order flow (dine-in billing, direct delivery,
 * QR ordering) and by menu availability checks, since a dish/combo recipe line may point at either
 * a grocery item or a prepared item.
 */
@Service
@RequiredArgsConstructor
public class IngredientAggregationService {

    private final InventoryService inventoryService;
    private final PreparedItemService preparedItemService;
    private final UnitService unitService;

    public static final class Requirements {
        private final Map<Long, BigDecimal> groceryQty = new HashMap<>();
        private final Map<Long, BigDecimal> preparedQty = new HashMap<>();

        public Map<Long, BigDecimal> groceryQty() {
            return groceryQty;
        }

        public Map<Long, BigDecimal> preparedQty() {
            return preparedQty;
        }

        public boolean isEmpty() {
            return groceryQty.isEmpty() && preparedQty.isEmpty();
        }

        public void mergeInto(Requirements target) {
            groceryQty.forEach((k, v) -> target.groceryQty.merge(k, v, BigDecimal::add));
            preparedQty.forEach((k, v) -> target.preparedQty.merge(k, v, BigDecimal::add));
        }
    }

    /** Aggregates the grocery/prepared item quantities needed to fulfil `qty` units of menuItem. */
    public Requirements aggregateForItem(MenuItem menuItem, int qty) {
        Requirements req = new Requirements();
        List<? extends RecipeLine> lines = menuItem.getDish() != null
                ? menuItem.getDish().getIngredients()
                : menuItem.getCombo() != null ? menuItem.getCombo().getIngredients() : List.of();
        for (RecipeLine ing : lines) {
            addLine(req, ing, qty);
        }
        return req;
    }

    private void addLine(Requirements req, RecipeLine ing, int qty) {
        BigDecimal multiplier = BigDecimal.valueOf(qty);
        if (ing.getGroceryItem() != null) {
            GroceryItem item = ing.getGroceryItem();
            BigDecimal needed = unitService.toStockUnit(item.getRecipeUnit(), item.getUnit(), ing.getQuantityRequired())
                    .setScale(3, RoundingMode.HALF_UP)
                    .multiply(multiplier);
            req.groceryQty().merge(item.getId(), needed, BigDecimal::add);
        } else {
            PreparedItem item = ing.getPreparedItem();
            BigDecimal needed = unitService.toStockUnit(item.getRecipeUnit(), item.getUnit(), ing.getQuantityRequired())
                    .setScale(3, RoundingMode.HALF_UP)
                    .multiply(multiplier);
            req.preparedQty().merge(item.getId(), needed, BigDecimal::add);
        }
    }

    /** Throws InsufficientStockException (listing every short item, grocery and prepared alike) if stock is short. */
    public void validateStock(Requirements req) {
        List<String> shortages = new ArrayList<>();
        shortages.addAll(inventoryService.findShortages(req.groceryQty()));
        shortages.addAll(preparedItemService.findShortages(req.preparedQty()));
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }
    }

    public void deduct(Requirements req, Long referenceId, Long userId) {
        for (Map.Entry<Long, BigDecimal> e : req.groceryQty().entrySet()) {
            inventoryService.deductStock(e.getKey(), e.getValue(), referenceId, userId);
        }
        for (Map.Entry<Long, BigDecimal> e : req.preparedQty().entrySet()) {
            preparedItemService.deductStock(e.getKey(), e.getValue(), referenceId, userId);
        }
    }

    /** Reverses a previous deduction (e.g. bill cancelled/reopened) by crediting stock back in. */
    public void reverse(Requirements req, String referenceType, Long referenceId, String notes, Long userId) {
        for (Map.Entry<Long, BigDecimal> e : req.groceryQty().entrySet()) {
            GroceryItem item = inventoryService.getItemById(e.getKey());
            inventoryService.recordMovement(item, MovementType.INBOUND, e.getValue(), referenceType, referenceId, notes, userId);
        }
        for (Map.Entry<Long, BigDecimal> e : req.preparedQty().entrySet()) {
            PreparedItem item = preparedItemService.getById(e.getKey());
            preparedItemService.recordMovement(item, MovementType.INBOUND, e.getValue(), referenceType, referenceId, notes, userId);
        }
    }

    /** Whether current stock (grocery and prepared) covers one unit of this menu item's recipe. */
    public boolean hasSufficientStock(MenuItem menuItem) {
        Requirements req = aggregateForItem(menuItem, 1);
        for (Map.Entry<Long, BigDecimal> e : req.groceryQty().entrySet()) {
            if (inventoryService.getItemById(e.getKey()).getQuantityInStock().compareTo(e.getValue()) < 0) {
                return false;
            }
        }
        for (Map.Entry<Long, BigDecimal> e : req.preparedQty().entrySet()) {
            if (preparedItemService.getById(e.getKey()).getQuantityInStock().compareTo(e.getValue()) < 0) {
                return false;
            }
        }
        return true;
    }

    /** Quantity of one recipe line's ingredient, converted to its stock unit — for detail-page display. */
    public BigDecimal requiredInStock(RecipeLine ing) {
        if (ing.getGroceryItem() != null) {
            GroceryItem item = ing.getGroceryItem();
            return unitService.toStockUnit(item.getRecipeUnit(), item.getUnit(), ing.getQuantityRequired());
        }
        PreparedItem item = ing.getPreparedItem();
        return unitService.toStockUnit(item.getRecipeUnit(), item.getUnit(), ing.getQuantityRequired());
    }
}
