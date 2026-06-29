package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.menu.dto.CreateMenuItemDto;
import in.bulletbeats.domain.menu.dto.UpdateMenuItemDto;
import in.bulletbeats.domain.menu.entity.*;
import in.bulletbeats.domain.menu.repository.*;
import in.bulletbeats.domain.shared.exception.InvalidMenuItemException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final ComboRepository comboRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final MenuItemAvailabilityLogRepository availabilityLogRepository;
    private final ImageStorageService imageStorageService;

    public List<MenuItem> getAllItems() {
        return menuItemRepository.findByIsActiveTrueOrderByCategoryDisplayOrderAscDisplayOrderAscNameAsc();
    }

    public List<MenuItem> getAllItemsForAdmin() {
        return menuItemRepository.findAllWithCategoryOrdered();
    }

    public List<MenuItem> getItemsByCategoryForAdmin(Long categoryId) {
        return menuItemRepository.findAllByCategoryIdWithCategoryOrdered(categoryId);
    }

    public MenuItem getItemById(Long id) {
        return menuItemRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
    }

    public List<MenuItem> getItemsByCategory(Long categoryId) {
        return menuItemRepository.findByCategoryIdAndIsActiveTrueOrderByDisplayOrderAscNameAsc(categoryId);
    }

    public List<MenuItem> getAllAvailableItems() {
        return menuItemRepository.findAllAvailableOrdered();
    }

    public List<MenuItem> searchAvailableItems(String q) {
        return menuItemRepository.searchAvailableItems(q);
    }

    public List<MenuItem> searchActiveItems(String q) {
        return menuItemRepository.searchActiveItems(q);
    }

    @Transactional
    public MenuItem createItem(CreateMenuItemDto dto, MultipartFile image, Long userId) {
        if ((dto.getDishId() == null) == (dto.getComboId() == null)) {
            throw new InvalidMenuItemException("Exactly one of dish or combo must be set");
        }
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + dto.getCategoryId()));
        Dish dish = dto.getDishId() != null
                ? dishRepository.findById(dto.getDishId())
                        .orElseThrow(() -> new ResourceNotFoundException("Dish not found: " + dto.getDishId()))
                : null;
        Combo combo = dto.getComboId() != null
                ? comboRepository.findById(dto.getComboId())
                        .orElseThrow(() -> new ResourceNotFoundException("Combo not found: " + dto.getComboId()))
                : null;

        String imagePath = null;
        String thumbnailPath = null;
        if (image != null && !image.isEmpty()) {
            ImageStorageService.StoredImage stored = imageStorageService.storeWithThumbnail(image, "menu");
            imagePath = stored.imagePath();
            thumbnailPath = stored.thumbnailPath();
        }

        MenuItem item = MenuItem.builder()
                .name(dto.getName())
                .category(category)
                .dish(dish)
                .combo(combo)
                .price(dto.getPrice())
                .isAvailable(true)
                .displayOrder(dto.getDisplayOrder())
                .imagePath(imagePath)
                .thumbnailPath(thumbnailPath)
                .isActive(true)
                .tenantId(1L)
                .build();
        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem updateItem(Long id, UpdateMenuItemDto dto, MultipartFile image, Long userId) {
        MenuItem item = getItemById(id);
        if ((dto.getDishId() == null) == (dto.getComboId() == null)) {
            throw new InvalidMenuItemException("Exactly one of dish or combo must be set");
        }
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + dto.getCategoryId()));
        Dish dish = dto.getDishId() != null
                ? dishRepository.findById(dto.getDishId())
                        .orElseThrow(() -> new ResourceNotFoundException("Dish not found: " + dto.getDishId()))
                : null;
        Combo combo = dto.getComboId() != null
                ? comboRepository.findById(dto.getComboId())
                        .orElseThrow(() -> new ResourceNotFoundException("Combo not found: " + dto.getComboId()))
                : null;

        if (image != null && !image.isEmpty()) {
            imageStorageService.delete(item.getImagePath());
            imageStorageService.delete(item.getThumbnailPath());
            ImageStorageService.StoredImage stored = imageStorageService.storeWithThumbnail(image, "menu");
            item.setImagePath(stored.imagePath());
            item.setThumbnailPath(stored.thumbnailPath());
        }

        item.setName(dto.getName());
        item.setCategory(category);
        item.setDish(dish);
        item.setCombo(combo);
        item.setPrice(dto.getPrice());
        item.setDisplayOrder(dto.getDisplayOrder());
        return menuItemRepository.save(item);
    }

    @Transactional
    public void updatePrice(Long id, BigDecimal newPrice, Long userId) {
        MenuItem item = getItemById(id);
        PriceHistory history = PriceHistory.builder()
                .menuItem(item)
                .oldPrice(item.getPrice())
                .newPrice(newPrice)
                .changedBy(userId)
                .build();
        priceHistoryRepository.save(history);
        item.setPrice(newPrice);
        menuItemRepository.save(item);
    }

    @Transactional
    public void updateAvailabilityOverride(Long menuItemId, Boolean newOverride, String reason, Long userId) {
        MenuItem item = getItemById(menuItemId);
        MenuItemAvailabilityLog log = MenuItemAvailabilityLog.builder()
                .menuItem(item)
                .changedBy(userId)
                .overrideBefore(item.getAvailabilityOverride())
                .overrideAfter(newOverride)
                .reason(reason)
                .build();
        availabilityLogRepository.save(log);
        item.setAvailabilityOverride(newOverride);
        item.setAvailable(newOverride != null ? newOverride : computeIngredientAvailability(item));
        menuItemRepository.save(item);
    }

    @Transactional
    public void recomputeAvailability(Long menuItemId) {
        MenuItem item = getItemById(menuItemId);
        if (item.getAvailabilityOverride() != null) {
            item.setAvailable(item.getAvailabilityOverride());
        } else {
            item.setAvailable(computeIngredientAvailability(item));
        }
        menuItemRepository.save(item);
    }

    @Transactional
    public void recomputeAllAutoMode() {
        menuItemRepository.findAutoModeItems()
                .forEach(item -> recomputeAvailability(item.getId()));
    }

    @Transactional
    public void deactivate(Long id) {
        MenuItem item = getItemById(id);
        imageStorageService.delete(item.getImagePath());
        imageStorageService.delete(item.getThumbnailPath());
        item.setActive(false);
        menuItemRepository.save(item);
    }

    @Transactional
    public void reactivate(Long id) {
        MenuItem item = getItemById(id);
        item.setActive(true);
        menuItemRepository.save(item);
    }

    public Optional<MenuItem> getPromotedItem() {
        return menuItemRepository.findActivePromoted();
    }

    @Transactional
    public void promote(Long id, String text) {
        menuItemRepository.findAllPromoted().forEach(item -> {
            item.setPromoted(false);
            item.setPromotionText(null);
            menuItemRepository.save(item);
        });
        MenuItem item = getItemById(id);
        item.setPromoted(true);
        item.setPromotionText(text);
        menuItemRepository.save(item);
    }

    @Transactional
    public void clearPromotion() {
        menuItemRepository.findAllPromoted().forEach(item -> {
            item.setPromoted(false);
            item.setPromotionText(null);
            menuItemRepository.save(item);
        });
    }

    public List<?> getIngredients(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
        if (item.getDish() != null) {
            return dishRepository.findById(item.getDish().getId())
                    .map(Dish::getIngredients)
                    .orElse(List.of());
        } else if (item.getCombo() != null) {
            return comboRepository.findById(item.getCombo().getId())
                    .map(Combo::getIngredients)
                    .orElse(List.of());
        }
        return List.of();
    }

    public List<PriceHistory> getPriceHistory(Long id) {
        return priceHistoryRepository.findByMenuItemIdOrderByChangedAtDesc(id);
    }

    public List<MenuItemAvailabilityLog> getAvailabilityLog(Long id) {
        return availabilityLogRepository.findByMenuItemIdOrderByChangedAtDesc(id);
    }

    private boolean computeIngredientAvailability(MenuItem item) {
        if (item.getDish() != null) {
            Dish dish = dishRepository.findById(item.getDish().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dish not found"));
            return dish.getIngredients().stream()
                    .allMatch(ing -> ing.getGroceryItem().getQuantityInStock()
                            .compareTo(ing.getQuantityRequired()) >= 0);
        } else {
            Combo combo = comboRepository.findById(item.getCombo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Combo not found"));
            return combo.getIngredients().stream()
                    .allMatch(ing -> ing.getGroceryItem().getQuantityInStock()
                            .compareTo(ing.getQuantityRequired()) >= 0);
        }
    }
}
