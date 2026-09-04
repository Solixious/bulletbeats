package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.PreparedItem;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.repository.PreparedItemRepository;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.inventory.service.PreparedItemService;
import in.bulletbeats.domain.inventory.service.UnitService;
import in.bulletbeats.domain.menu.dto.CreateDishDto;
import in.bulletbeats.domain.menu.dto.DishIngredientDto;
import in.bulletbeats.domain.menu.dto.UpdateDishDto;
import in.bulletbeats.domain.menu.entity.Dish;
import in.bulletbeats.domain.menu.entity.DishIngredient;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.repository.DishRepository;
import in.bulletbeats.domain.menu.repository.MenuItemRepository;
import in.bulletbeats.domain.shared.exception.DishInUseException;
import in.bulletbeats.domain.shared.exception.MissingUnitConversionException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DishService {

    private final DishRepository dishRepository;
    private final GroceryItemRepository groceryItemRepository;
    private final PreparedItemRepository preparedItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final UnitService unitService;
    private final InventoryService inventoryService;
    private final PreparedItemService preparedItemService;
    private final MenuService menuService;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Dish> getAll() {
        return dishRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public List<Dish> getInactive() {
        return dishRepository.findByIsActiveFalseOrderByNameAsc();
    }

    public Dish getById(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found with id: " + id));
    }

    @Transactional
    public Dish create(CreateDishDto dto) {
        if (dishRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("A dish with this name already exists: " + dto.getName());
        }
        Dish dish = Dish.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .recipeNotes(dto.getRecipeNotes())
                .prepTimeMinutes(dto.getPrepTimeMinutes())
                .dishType(dto.getDishType())
                .isActive(true)
                .tenantId(1L)
                .build();
        if (dto.getIngredients() != null) {
            dto.getIngredients().forEach(i -> dish.getIngredients().add(buildIngredient(dish, i)));
        }
        Dish saved = dishRepository.save(dish);
        recomputeMenuItemAvailability(saved.getId());
        return saved;
    }

    @Transactional
    public Dish update(Long id, UpdateDishDto dto) {
        Dish dish = getById(id);
        if (!dish.getName().equalsIgnoreCase(dto.getName())
                && dishRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("A dish with this name already exists: " + dto.getName());
        }
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription());
        dish.setRecipeNotes(dto.getRecipeNotes());
        dish.setPrepTimeMinutes(dto.getPrepTimeMinutes());
        dish.setDishType(dto.getDishType());
        dish.getIngredients().clear();
        entityManager.flush(); // send DELETEs to DB before INSERTs to avoid unique constraint violation
        if (dto.getIngredients() != null) {
            dto.getIngredients().forEach(i -> dish.getIngredients().add(buildIngredient(dish, i)));
        }
        Dish saved = dishRepository.save(dish);
        recomputeMenuItemAvailability(saved.getId());
        return saved;
    }

    /** Re-checks auto-mode availability for every menu item built on this dish (recipe just changed). */
    private void recomputeMenuItemAvailability(Long dishId) {
        for (MenuItem item : menuItemRepository.findByDishId(dishId)) {
            menuService.recomputeAvailability(item.getId());
        }
    }

    @Transactional
    public Dish updateRecipeNotes(Long dishId, String notes) {
        Dish dish = getById(dishId);
        String trimmed = (notes != null) ? notes.trim() : "";
        dish.setRecipeNotes(trimmed.isEmpty() ? null : trimmed);
        return dishRepository.save(dish);
    }

    @Transactional
    public void reactivate(Long id) {
        Dish dish = getById(id);
        dish.setActive(true);
        dishRepository.save(dish);
    }

    @Transactional
    public void deactivate(Long id) {
        Dish dish = getById(id);
        boolean inUse = menuItemRepository.findByDishId(id).stream().anyMatch(MenuItem::isActive);
        if (inUse) {
            throw new DishInUseException(id);
        }
        dish.setActive(false);
        dishRepository.save(dish);
    }

    /** Total recipe cost across all ingredients (grocery and prepared alike). Null if any ingredient cost is unknown. */
    public BigDecimal computeCost(Dish dish) {
        BigDecimal total = BigDecimal.ZERO;
        for (DishIngredient ing : dish.getIngredients()) {
            BigDecimal costPerRecipeUnit = ing.getGroceryItem() != null
                    ? inventoryService.computeCostPerRecipeUnit(ing.getGroceryItem())
                    : preparedItemService.computeCostPerRecipeUnit(ing.getPreparedItem());
            if (costPerRecipeUnit == null) return null;
            total = total.add(costPerRecipeUnit.multiply(ing.getQuantityRequired()));
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private DishIngredient buildIngredient(Dish dish, DishIngredientDto dto) {
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
            return DishIngredient.builder()
                    .dish(dish)
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
            return DishIngredient.builder()
                    .dish(dish)
                    .preparedItem(preparedItem)
                    .quantityRequired(dto.getQuantityRequired())
                    .build();
        }
        throw new IllegalArgumentException("Select an ingredient");
    }
}
