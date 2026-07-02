package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.menu.dto.CreateDishDto;
import in.bulletbeats.domain.menu.dto.DishIngredientDto;
import in.bulletbeats.domain.menu.dto.UpdateDishDto;
import in.bulletbeats.domain.menu.entity.Dish;
import in.bulletbeats.domain.menu.entity.DishIngredient;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.repository.DishRepository;
import in.bulletbeats.domain.menu.repository.MenuItemRepository;
import in.bulletbeats.domain.shared.exception.DishInUseException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DishService {

    private final DishRepository dishRepository;
    private final GroceryItemRepository groceryItemRepository;
    private final MenuItemRepository menuItemRepository;

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
                .isActive(true)
                .tenantId(1L)
                .build();
        if (dto.getIngredients() != null) {
            dto.getIngredients().forEach(i -> dish.getIngredients().add(buildIngredient(dish, i)));
        }
        return dishRepository.save(dish);
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
        dish.getIngredients().clear();
        entityManager.flush(); // send DELETEs to DB before INSERTs to avoid unique constraint violation
        if (dto.getIngredients() != null) {
            dto.getIngredients().forEach(i -> dish.getIngredients().add(buildIngredient(dish, i)));
        }
        return dishRepository.save(dish);
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

    private DishIngredient buildIngredient(Dish dish, DishIngredientDto dto) {
        GroceryItem groceryItem = groceryItemRepository.findById(dto.getGroceryItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grocery item not found with id: " + dto.getGroceryItemId()));
        return DishIngredient.builder()
                .dish(dish)
                .groceryItem(groceryItem)
                .quantityRequired(dto.getQuantityRequired())
                .build();
    }
}
