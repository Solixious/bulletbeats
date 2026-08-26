package in.bulletbeats.domain.inventory.service;

import in.bulletbeats.domain.inventory.dto.CreatePreparedItemDto;
import in.bulletbeats.domain.inventory.dto.PrepareBatchDto;
import in.bulletbeats.domain.inventory.dto.PreparedItemIngredientDto;
import in.bulletbeats.domain.inventory.dto.PreparedItemStockAdjustmentDto;
import in.bulletbeats.domain.inventory.dto.UpdatePreparedItemDto;
import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.PreparedItem;
import in.bulletbeats.domain.inventory.entity.PreparedItemIngredient;
import in.bulletbeats.domain.inventory.entity.PreparedItemStockMovement;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.repository.PreparedItemRepository;
import in.bulletbeats.domain.inventory.repository.PreparedItemStockMovementRepository;
import in.bulletbeats.domain.menu.repository.ComboRepository;
import in.bulletbeats.domain.menu.repository.DishRepository;
import in.bulletbeats.domain.shared.enums.MovementType;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.shared.exception.MissingUnitConversionException;
import in.bulletbeats.domain.shared.exception.PreparedItemInUseException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreparedItemService {

    private final PreparedItemRepository preparedItemRepository;
    private final PreparedItemStockMovementRepository preparedItemStockMovementRepository;
    private final GroceryItemRepository groceryItemRepository;
    private final InventoryService inventoryService;
    private final UnitService unitService;
    private final DishRepository dishRepository;
    private final ComboRepository comboRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<PreparedItem> getAll() {
        return preparedItemRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public PreparedItem getById(Long id) {
        return preparedItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prepared item not found with id: " + id));
    }

    /** Cost of one full batch, from current grocery item costs. Null if any ingredient cost is unknown. */
    public BigDecimal computeCostPerBatch(PreparedItem item) {
        BigDecimal total = BigDecimal.ZERO;
        for (PreparedItemIngredient ing : item.getIngredients()) {
            BigDecimal costPerRecipeUnit = inventoryService.computeCostPerRecipeUnit(ing.getGroceryItem());
            if (costPerRecipeUnit == null) return null;
            total = total.add(costPerRecipeUnit.multiply(ing.getQuantityRequired()));
        }
        return total.setScale(4, RoundingMode.HALF_UP);
    }

    /** Cost per stock unit (e.g. per kg produced). Null if not computable. */
    public BigDecimal computeCostPerUnit(PreparedItem item) {
        BigDecimal costPerBatch = computeCostPerBatch(item);
        if (costPerBatch == null || item.getBatchYieldQuantity() == null
                || item.getBatchYieldQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return costPerBatch.divide(item.getBatchYieldQuantity(), 4, RoundingMode.HALF_UP);
    }

    /** Cost per the item's recipe unit — what a dish/combo recipe consuming this prepared item should be priced at. */
    public BigDecimal computeCostPerRecipeUnit(PreparedItem item) {
        BigDecimal costPerUnit = computeCostPerUnit(item);
        if (costPerUnit == null) return null;
        BigDecimal factor = unitService.conversionFactor(item.getUnit(), item.getRecipeUnit());
        if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) return null;
        return costPerUnit.divide(factor, 4, RoundingMode.HALF_UP);
    }

    @Transactional
    public PreparedItem create(CreatePreparedItemDto dto) {
        if (preparedItemRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("A prepared item with this name already exists: " + dto.getName());
        }
        PreparedItem item = PreparedItem.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .prepTimeMinutes(dto.getPrepTimeMinutes())
                .unit(dto.getUnit().toLowerCase())
                .minorUnit(blankToNull(dto.getMinorUnit()))
                .batchYieldQuantity(dto.getBatchYieldQuantity())
                .quantityInStock(BigDecimal.ZERO)
                .minThreshold(dto.getMinThreshold())
                .isActive(true)
                .tenantId(1L)
                .build();
        if (dto.getIngredients() != null) {
            dto.getIngredients().forEach(i -> item.getIngredients().add(buildIngredient(item, i)));
        }
        return preparedItemRepository.save(item);
    }

    @Transactional
    public PreparedItem update(Long id, UpdatePreparedItemDto dto) {
        PreparedItem item = getById(id);
        if (!item.getName().equalsIgnoreCase(dto.getName())
                && preparedItemRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("A prepared item with this name already exists: " + dto.getName());
        }
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setPrepTimeMinutes(dto.getPrepTimeMinutes());
        item.setUnit(dto.getUnit().toLowerCase());
        item.setMinorUnit(blankToNull(dto.getMinorUnit()));
        item.setBatchYieldQuantity(dto.getBatchYieldQuantity());
        item.setMinThreshold(dto.getMinThreshold());
        item.getIngredients().clear();
        entityManager.flush(); // send DELETEs before INSERTs to avoid unique constraint violation
        if (dto.getIngredients() != null) {
            dto.getIngredients().forEach(i -> item.getIngredients().add(buildIngredient(item, i)));
        }
        return preparedItemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        PreparedItem item = getById(id);
        if (preparedItemStockMovementRepository.existsByPreparedItemId(id)
                || dishRepository.existsByIngredientsPreparedItemId(id)
                || comboRepository.existsByIngredientsPreparedItemId(id)) {
            throw new PreparedItemInUseException(item.getName());
        }
        preparedItemRepository.delete(item);
    }

    /** Consumes grocery item stock per the prep recipe and adds the resulting yield to this item's stock. */
    @Transactional
    public PreparedItem prepareBatch(Long id, PrepareBatchDto dto, Long userId) {
        PreparedItem item = getById(id);
        BigDecimal batches = dto.getBatches();

        Map<Long, BigDecimal> required = new HashMap<>();
        for (PreparedItemIngredient ing : item.getIngredients()) {
            GroceryItem gi = ing.getGroceryItem();
            BigDecimal qty = unitService.toStockUnit(gi.getRecipeUnit(), gi.getUnit(),
                            ing.getQuantityRequired().multiply(batches))
                    .setScale(3, RoundingMode.HALF_UP);
            required.merge(gi.getId(), qty, BigDecimal::add);
        }
        inventoryService.validateStock(required);
        for (Map.Entry<Long, BigDecimal> entry : required.entrySet()) {
            GroceryItem gi = inventoryService.getItemById(entry.getKey());
            inventoryService.recordMovement(gi, MovementType.OUTBOUND, entry.getValue(),
                    "PREP_BATCH", item.getId(), "Consumed to prepare " + item.getName(), userId);
        }

        BigDecimal yield = item.getBatchYieldQuantity().multiply(batches).setScale(3, RoundingMode.HALF_UP);
        BigDecimal before = item.getQuantityInStock();
        BigDecimal after = before.add(yield);
        item.setQuantityInStock(after);
        preparedItemRepository.save(item);

        preparedItemStockMovementRepository.save(PreparedItemStockMovement.builder()
                .preparedItem(item)
                .movementType(MovementType.INBOUND)
                .quantity(yield)
                .stockBefore(before)
                .stockAfter(after)
                .referenceType("PREP_BATCH")
                .notes(dto.getNotes())
                .createdBy(userId)
                .build());

        return item;
    }

    @Transactional
    public PreparedItem adjustStock(Long id, PreparedItemStockAdjustmentDto dto, Long userId) {
        PreparedItem item = getById(id);
        BigDecimal before = item.getQuantityInStock();
        BigDecimal delta = dto.getQuantity();

        BigDecimal after = switch (dto.getMovementType()) {
            case INBOUND, ADJUSTMENT -> before.add(delta);
            case OUTBOUND, WASTAGE -> before.subtract(delta);
        };

        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientStockException(List.of(
                    item.getName() + ": requested " + delta + " but only " + before + " available"));
        }

        item.setQuantityInStock(after);
        preparedItemRepository.save(item);

        preparedItemStockMovementRepository.save(PreparedItemStockMovement.builder()
                .preparedItem(item)
                .movementType(dto.getMovementType())
                .quantity(delta)
                .stockBefore(before)
                .stockAfter(after)
                .notes(dto.getNotes())
                .createdBy(userId)
                .build());

        return item;
    }

    /** Deducts stock for a sold dish/combo that used this prepared item, or reverses via INBOUND. */
    @Transactional
    public void recordMovement(PreparedItem item, MovementType type, BigDecimal qty,
                                String referenceType, Long referenceId, String notes, Long userId) {
        BigDecimal before = item.getQuantityInStock();
        BigDecimal after = switch (type) {
            case INBOUND, ADJUSTMENT -> before.add(qty);
            case OUTBOUND, WASTAGE -> before.subtract(qty);
        };
        item.setQuantityInStock(after);
        preparedItemRepository.save(item);

        preparedItemStockMovementRepository.save(PreparedItemStockMovement.builder()
                .preparedItem(item)
                .movementType(type)
                .quantity(qty)
                .stockBefore(before)
                .stockAfter(after)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .notes(notes)
                .createdBy(userId)
                .build());
    }

    @Transactional
    public void deductStock(Long preparedItemId, BigDecimal qty, Long billId, Long userId) {
        PreparedItem item = getById(preparedItemId);
        BigDecimal before = item.getQuantityInStock();
        if (before.compareTo(qty) < 0) {
            throw new InsufficientStockException(List.of(
                    item.getName() + ": need " + qty + " " + item.getUnit() + ", have " + before));
        }
        recordMovement(item, MovementType.OUTBOUND, qty, "BILL", billId, null, userId);
    }

    public Page<PreparedItemStockMovement> getMovementsForItem(Long itemId, Pageable pageable) {
        return preparedItemStockMovementRepository.findByPreparedItemId(itemId, pageable);
    }

    /** Shortage messages for any required prepared item (by id, in stock unit) whose stock falls short. */
    public List<String> findShortages(Map<Long, BigDecimal> required) {
        List<String> shortages = new java.util.ArrayList<>();
        required.forEach((id, qty) -> {
            PreparedItem item = getById(id);
            if (item.getQuantityInStock().compareTo(qty) < 0) {
                shortages.add(item.getName() + ": need " + qty + " " + item.getUnit()
                        + ", have " + item.getQuantityInStock());
            }
        });
        return shortages;
    }

    private PreparedItemIngredient buildIngredient(PreparedItem item, PreparedItemIngredientDto dto) {
        GroceryItem groceryItem = groceryItemRepository.findById(dto.getGroceryItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grocery item not found with id: " + dto.getGroceryItemId()));
        String recipeUnit = groceryItem.getRecipeUnit();
        if (!recipeUnit.equalsIgnoreCase(groceryItem.getUnit())
                && unitService.conversionFactor(recipeUnit, groceryItem.getUnit()) == null) {
            throw new MissingUnitConversionException(recipeUnit, groceryItem.getUnit());
        }
        return PreparedItemIngredient.builder()
                .preparedItem(item)
                .groceryItem(groceryItem)
                .quantityRequired(dto.getQuantityRequired())
                .build();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toLowerCase();
    }
}
