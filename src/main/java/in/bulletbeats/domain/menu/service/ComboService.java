package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.PreparedItem;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.repository.PreparedItemRepository;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.inventory.service.PreparedItemService;
import in.bulletbeats.domain.inventory.service.UnitService;
import in.bulletbeats.domain.menu.dto.ComboIngredientDto;
import in.bulletbeats.domain.menu.dto.CreateComboDto;
import in.bulletbeats.domain.menu.dto.UpdateComboDto;
import in.bulletbeats.domain.menu.entity.Combo;
import in.bulletbeats.domain.menu.entity.ComboIngredient;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.repository.ComboRepository;
import in.bulletbeats.domain.menu.repository.MenuItemRepository;
import in.bulletbeats.domain.shared.exception.ComboInUseException;
import in.bulletbeats.domain.shared.exception.MissingUnitConversionException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComboService {

    private final ComboRepository comboRepository;
    private final GroceryItemRepository groceryItemRepository;
    private final PreparedItemRepository preparedItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final UnitService unitService;
    private final InventoryService inventoryService;
    private final PreparedItemService preparedItemService;

    public List<Combo> getAll() {
        return comboRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public Combo getById(Long id) {
        return comboRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Combo not found with id: " + id));
    }

    @Transactional
    public Combo create(CreateComboDto dto) {
        if (comboRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("A combo with this name already exists: " + dto.getName());
        }
        Combo combo = Combo.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(true)
                .tenantId(1L)
                .build();
        dto.getIngredients().forEach(i -> combo.getIngredients().add(buildIngredient(combo, i)));
        return comboRepository.save(combo);
    }

    @Transactional
    public Combo update(Long id, UpdateComboDto dto) {
        Combo combo = getById(id);
        if (!combo.getName().equalsIgnoreCase(dto.getName())
                && comboRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("A combo with this name already exists: " + dto.getName());
        }
        combo.setName(dto.getName());
        combo.setDescription(dto.getDescription());
        combo.getIngredients().clear();
        dto.getIngredients().forEach(i -> combo.getIngredients().add(buildIngredient(combo, i)));
        return comboRepository.save(combo);
    }

    @Transactional
    public void deactivate(Long id) {
        Combo combo = getById(id);
        boolean inUse = menuItemRepository.findByComboId(id).stream().anyMatch(MenuItem::isActive);
        if (inUse) {
            throw new ComboInUseException(id);
        }
        combo.setActive(false);
        comboRepository.save(combo);
    }

    /** Total recipe cost across all ingredients (grocery and prepared alike). Null if any ingredient cost is unknown. */
    public BigDecimal computeCost(Combo combo) {
        BigDecimal total = BigDecimal.ZERO;
        for (ComboIngredient ing : combo.getIngredients()) {
            BigDecimal costPerRecipeUnit = ing.getGroceryItem() != null
                    ? inventoryService.computeCostPerRecipeUnit(ing.getGroceryItem())
                    : preparedItemService.computeCostPerRecipeUnit(ing.getPreparedItem());
            if (costPerRecipeUnit == null) return null;
            total = total.add(costPerRecipeUnit.multiply(ing.getQuantityRequired()));
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private ComboIngredient buildIngredient(Combo combo, ComboIngredientDto dto) {
        if (dto.getGroceryItemId() != null && dto.getPreparedItemId() != null) {
            throw new IllegalArgumentException("Select either a grocery item or a prepared item, not both");
        }
        if (dto.getGroceryItemId() != null) {
            GroceryItem groceryItem = groceryItemRepository.findById(dto.getGroceryItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Grocery item not found with id: " + dto.getGroceryItemId()));
            String recipeUnit = groceryItem.getRecipeUnit();
            if (!recipeUnit.equalsIgnoreCase(groceryItem.getUnit())
                    && unitService.conversionFactor(recipeUnit, groceryItem.getUnit()) == null) {
                throw new MissingUnitConversionException(recipeUnit, groceryItem.getUnit());
            }
            return ComboIngredient.builder()
                    .combo(combo)
                    .groceryItem(groceryItem)
                    .quantityRequired(dto.getQuantityRequired())
                    .build();
        }
        if (dto.getPreparedItemId() != null) {
            PreparedItem preparedItem = preparedItemRepository.findById(dto.getPreparedItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prepared item not found with id: " + dto.getPreparedItemId()));
            String recipeUnit = preparedItem.getRecipeUnit();
            if (!recipeUnit.equalsIgnoreCase(preparedItem.getUnit())
                    && unitService.conversionFactor(recipeUnit, preparedItem.getUnit()) == null) {
                throw new MissingUnitConversionException(recipeUnit, preparedItem.getUnit());
            }
            return ComboIngredient.builder()
                    .combo(combo)
                    .preparedItem(preparedItem)
                    .quantityRequired(dto.getQuantityRequired())
                    .build();
        }
        throw new IllegalArgumentException("Select an ingredient");
    }
}
