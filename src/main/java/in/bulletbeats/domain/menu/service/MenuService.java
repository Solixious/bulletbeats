package in.bulletbeats.domain.menu.service;

import in.bulletbeats.domain.menu.dto.CategoryNode;
import in.bulletbeats.domain.menu.dto.CreateMenuItemDto;
import in.bulletbeats.domain.menu.dto.UpdateMenuItemDto;
import in.bulletbeats.domain.menu.entity.*;
import in.bulletbeats.domain.menu.repository.*;
import in.bulletbeats.domain.platform.entity.OnlinePlatform;
import in.bulletbeats.domain.platform.service.OnlinePlatformService;
import in.bulletbeats.domain.shared.exception.InvalidMenuItemException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final CategoryService categoryService;
    private final MenuItemPlatformPriceRepository menuItemPlatformPriceRepository;
    private final OnlinePlatformService onlinePlatformService;
    private final IngredientAggregationService ingredientAggregationService;

    public List<MenuItem> getAllItems() {
        return menuItemRepository.findByIsActiveTrueOrderByCategoryDisplayOrderAscDisplayOrderAscNameAsc();
    }

    public List<MenuItem> getAllItemsForAdmin() {
        return menuItemRepository.findAllWithCategoryOrdered();
    }

    public List<MenuItem> getItemsByCategoryForAdmin(Long categoryId) {
        return menuItemRepository.findAllByCategoryIdWithCategoryOrdered(categoryId);
    }

    @Cacheable(value = "menuItems", key = "#id")
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

    /**
     * Active items ordered by the category tree (top-level category, then its subcategories in order)
     * rather than the raw {@code category.displayOrder} column, which is scoped independently per
     * parent and collides across unrelated categories/subcategories when compared directly.
     */
    public List<MenuItem> getAllItemsTreeOrdered() {
        return treeOrder(getAllItems());
    }

    public List<MenuItem> searchActiveItemsTreeOrdered(String q) {
        return treeOrder(searchActiveItems(q));
    }

    /**
     * Admin "All" view: includes inactive items too (unlike {@link #getAllItemsTreeOrdered()}),
     * and — since admins need to be able to find/reactivate items even under a since-deactivated
     * category — items whose category isn't in the active tree are appended at the end rather
     * than silently dropped.
     */
    public List<MenuItem> getAllItemsForAdminTreeOrdered() {
        List<MenuItem> all = getAllItemsForAdmin();
        List<CategoryNode> tree = categoryService.getActiveCategoryTree();
        Map<Long, List<MenuItem>> byCategory = all.stream()
                .filter(item -> item.getCategory() != null)
                .collect(Collectors.groupingBy(item -> item.getCategory().getId()));

        List<MenuItem> ordered = new ArrayList<>();
        java.util.Set<Long> consumed = new java.util.HashSet<>();
        for (CategoryNode node : tree) {
            ordered.addAll(byCategory.getOrDefault(node.category().getId(), List.of()));
            consumed.add(node.category().getId());
            for (Category sub : node.subcategories()) {
                ordered.addAll(byCategory.getOrDefault(sub.getId(), List.of()));
                consumed.add(sub.getId());
            }
        }
        byCategory.entrySet().stream()
                .filter(e -> !consumed.contains(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> ordered.addAll(e.getValue()));
        return ordered;
    }

    private List<MenuItem> treeOrder(List<MenuItem> items) {
        List<CategoryNode> tree = categoryService.getActiveCategoryTree();
        Map<Long, List<MenuItem>> byCategory = items.stream()
                .filter(item -> item.getCategory() != null)
                .collect(Collectors.groupingBy(item -> item.getCategory().getId()));

        List<MenuItem> ordered = new ArrayList<>();
        for (CategoryNode node : tree) {
            ordered.addAll(byCategory.getOrDefault(node.category().getId(), List.of()));
            for (Category sub : node.subcategories()) {
                ordered.addAll(byCategory.getOrDefault(sub.getId(), List.of()));
            }
        }
        return ordered;
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
        long imageVersion = 0L;
        if (image != null && !image.isEmpty()) {
            ImageStorageService.StoredImage stored = imageStorageService.storeWithThumbnail(image, "menu");
            imagePath = stored.imagePath();
            thumbnailPath = stored.thumbnailPath();
            imageVersion = System.currentTimeMillis();
        }

        applyDescription(dish, combo, dto.getDescription());

        MenuItem item = MenuItem.builder()
                .name(dto.getName())
                .category(category)
                .dish(dish)
                .combo(combo)
                .price(dto.getPrice())
                .quantityLabel(dto.getQuantityLabel())
                .isAvailable(true)
                .displayOrder(dto.getDisplayOrder())
                .imagePath(imagePath)
                .thumbnailPath(thumbnailPath)
                .imageVersion(imageVersion)
                .isActive(true)
                .tenantId(1L)
                .build();
        MenuItem saved = menuItemRepository.save(item);
        syncPlatformPrices(saved, dto.getPlatformPrices());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "menuItems", key = "#id")
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
            item.setImageVersion(System.currentTimeMillis());
        }

        applyDescription(dish, combo, dto.getDescription());

        item.setName(dto.getName());
        item.setCategory(category);
        item.setDish(dish);
        item.setCombo(combo);
        item.setPrice(dto.getPrice());
        item.setQuantityLabel(dto.getQuantityLabel());
        item.setDisplayOrder(dto.getDisplayOrder());
        MenuItem saved = menuItemRepository.save(item);
        syncPlatformPrices(saved, dto.getPlatformPrices());
        return saved;
    }

    /**
     * Resolves the price to charge for {@code item} when sold via {@code platform}: the
     * platform-specific override if one is configured, otherwise the base menu price.
     * {@code platform} may be null (non-online-order bills), in which case the base price
     * is always used.
     */
    public BigDecimal resolvePrice(MenuItem item, OnlinePlatform platform) {
        if (platform == null) {
            return item.getPrice();
        }
        return menuItemPlatformPriceRepository.findByMenuItemIdAndPlatformId(item.getId(), platform.getId())
                .map(MenuItemPlatformPrice::getPrice)
                .orElse(item.getPrice());
    }

    public Map<Long, String> getPlatformPricesAsStrings(Long menuItemId) {
        return menuItemPlatformPriceRepository.findByMenuItemId(menuItemId).stream()
                .collect(Collectors.toMap(
                        p -> p.getPlatform().getId(),
                        p -> p.getPrice().toPlainString()));
    }

    private void syncPlatformPrices(MenuItem item, Map<Long, String> rawPrices) {
        if (rawPrices == null) {
            return;
        }
        for (Map.Entry<Long, String> entry : rawPrices.entrySet()) {
            Long platformId = entry.getKey();
            String raw = entry.getValue() == null ? null : entry.getValue().trim();
            Optional<MenuItemPlatformPrice> existing =
                    menuItemPlatformPriceRepository.findByMenuItemIdAndPlatformId(item.getId(), platformId);

            if (raw == null || raw.isEmpty()) {
                existing.ifPresent(menuItemPlatformPriceRepository::delete);
                continue;
            }

            BigDecimal price;
            try {
                price = new BigDecimal(raw);
            } catch (NumberFormatException e) {
                continue;
            }

            if (existing.isPresent()) {
                existing.get().setPrice(price);
                menuItemPlatformPriceRepository.save(existing.get());
            } else {
                OnlinePlatform platform;
                try {
                    platform = onlinePlatformService.getById(platformId);
                } catch (ResourceNotFoundException e) {
                    continue;
                }
                menuItemPlatformPriceRepository.save(MenuItemPlatformPrice.builder()
                        .menuItem(item)
                        .platform(platform)
                        .price(price)
                        .build());
            }
        }
    }

    private void applyDescription(Dish dish, Combo combo, String description) {
        if (description == null || description.isBlank()) {
            return;
        }
        if (dish != null) {
            dish.setDescription(description);
            dishRepository.save(dish);
        } else if (combo != null) {
            combo.setDescription(description);
            comboRepository.save(combo);
        }
    }

    @Transactional
    @CacheEvict(value = "menuItems", key = "#id")
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
    @CacheEvict(value = "menuItems", key = "#menuItemId")
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
    @CacheEvict(value = "menuItems", key = "#menuItemId")
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
    @CacheEvict(value = "menuItems", allEntries = true)
    public void recomputeAllAutoMode() {
        List<MenuItem> items = menuItemRepository.findAutoModeItemsWithRecipes();
        for (MenuItem item : items) {
            item.setAvailable(computeIngredientAvailability(item));
        }
        menuItemRepository.saveAll(items);
    }

    @Transactional
    public void reorderItems(List<Long> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            MenuItem item = getItemById(orderedIds.get(i));
            item.setDisplayOrder(i);
            menuItemRepository.save(item);
        }
    }

    @Transactional
    @CacheEvict(value = "menuItems", key = "#id")
    public void deactivate(Long id) {
        MenuItem item = getItemById(id);
        imageStorageService.delete(item.getImagePath());
        imageStorageService.delete(item.getThumbnailPath());
        item.setActive(false);
        menuItemRepository.save(item);
    }

    @Transactional
    @CacheEvict(value = "menuItems", key = "#id")
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

    public Map<Long, BigDecimal> getRequiredInStock(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
        Map<Long, BigDecimal> result = new HashMap<>();
        if (item.getDish() != null) {
            for (DishIngredient ing : item.getDish().getIngredients()) {
                result.put(ing.getId(), ingredientAggregationService.requiredInStock(ing));
            }
        } else if (item.getCombo() != null) {
            for (ComboIngredient ing : item.getCombo().getIngredients()) {
                result.put(ing.getId(), ingredientAggregationService.requiredInStock(ing));
            }
        }
        return result;
    }

    public List<PriceHistory> getPriceHistory(Long id) {
        return priceHistoryRepository.findByMenuItemIdOrderByChangedAtDesc(id);
    }

    public List<MenuItemAvailabilityLog> getAvailabilityLog(Long id) {
        return availabilityLogRepository.findByMenuItemIdOrderByChangedAtDesc(id);
    }

    private boolean computeIngredientAvailability(MenuItem item) {
        if (item.getDish() == null && item.getCombo() == null) {
            return true;
        }
        return ingredientAggregationService.hasSufficientStock(item);
    }
}
