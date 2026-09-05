package in.bulletbeats.domain.inventory.service;

import in.bulletbeats.domain.inventory.dto.CreateGroceryItemDto;
import in.bulletbeats.domain.inventory.dto.StockAdjustmentDto;
import in.bulletbeats.domain.inventory.dto.UpdateGroceryItemDto;
import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.InventoryCategory;
import in.bulletbeats.domain.inventory.entity.ReplenishmentRequest;
import in.bulletbeats.domain.inventory.entity.StockMovement;
import in.bulletbeats.domain.inventory.entity.Supplier;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.repository.InventoryCategoryRepository;
import in.bulletbeats.domain.inventory.repository.PreparedItemRepository;
import in.bulletbeats.domain.inventory.repository.PurchaseOrderRepository;
import in.bulletbeats.domain.inventory.repository.ReplenishmentRequestRepository;
import in.bulletbeats.domain.inventory.repository.StockMovementRepository;
import in.bulletbeats.domain.inventory.repository.SupplierRepository;
import in.bulletbeats.domain.shared.enums.MovementType;
import in.bulletbeats.domain.shared.enums.PurchaseOrderStatus;
import in.bulletbeats.domain.shared.enums.ReplenishmentStatus;
import in.bulletbeats.domain.shared.exception.DuplicateGroceryItemException;
import in.bulletbeats.domain.shared.exception.GroceryItemInUseException;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.menu.repository.ComboRepository;
import in.bulletbeats.domain.menu.repository.DishRepository;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private static final List<PurchaseOrderStatus> ACTIVE_PO_STATUSES =
            List.of(PurchaseOrderStatus.PENDING, PurchaseOrderStatus.APPROVED, PurchaseOrderStatus.ORDERED);

    private final GroceryItemRepository groceryItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ReplenishmentRequestRepository replenishmentRequestRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final DishRepository dishRepository;
    private final ComboRepository comboRepository;
    private final PreparedItemRepository preparedItemRepository;
    private final InventoryCategoryRepository inventoryCategoryRepository;
    private final UnitService unitService;

    @Lazy
    @Autowired
    private MenuService menuService;

    public long getLowStockCount() {
        List<GroceryItem> lowItems = groceryItemRepository.findLowStockItems();
        if (lowItems.isEmpty()) return 0;
        Set<Long> onOrderIds = purchaseOrderRepository.findGroceryItemIdsInActiveOrders(ACTIVE_PO_STATUSES);
        return lowItems.stream().filter(i -> !onOrderIds.contains(i.getId())).count();
    }

    public Set<Long> getOnOrderGroceryItemIds() {
        return purchaseOrderRepository.findGroceryItemIdsInActiveOrders(ACTIVE_PO_STATUSES);
    }

    public List<GroceryItem> getAllItems() {
        return groceryItemRepository.findAllActiveWithSupplier();
    }

    public GroceryItem getItemById(Long id) {
        return groceryItemRepository.findByIdWithSupplier(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grocery item not found with id: " + id));
    }

    public List<GroceryItem> getItemsWithLowStockFlag() {
        return groceryItemRepository.findAllActiveWithSupplier().stream()
                .sorted(Comparator.comparing(GroceryItem::isLowStock).reversed())
                .toList();
    }

    public BigDecimal computeCostPerMinorUnit(GroceryItem item) {
        BigDecimal costPerUnit = item.getCostPerUnit();
        if (costPerUnit == null || item.getMinorUnit() == null) return null;
        BigDecimal factor = unitService.conversionFactor(item.getPackUnit(), item.getMinorUnit());
        if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) return null;
        return costPerUnit.divide(factor, 4, RoundingMode.HALF_UP);
    }

    /** Cost per the item's recipe unit (its minor unit if configured, else its stock unit). Null if not computable. */
    public BigDecimal computeCostPerRecipeUnit(GroceryItem item) {
        BigDecimal costPerUnit = item.getCostPerUnit();
        if (costPerUnit == null) return null;
        BigDecimal factor = unitService.conversionFactor(item.getPackUnit(), item.getRecipeUnit());
        if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) return null;
        return costPerUnit.divide(factor, 4, RoundingMode.HALF_UP);
    }

    /** Shortage messages for any required grocery item (by id, in stock unit) whose stock falls short. */
    public List<String> findShortages(Map<Long, BigDecimal> required) {
        if (required.isEmpty()) return List.of();
        Map<Long, GroceryItem> byId = groceryItemRepository.findAllById(required.keySet())
                .stream().collect(java.util.stream.Collectors.toMap(GroceryItem::getId, g -> g));
        List<String> shortages = new java.util.ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : required.entrySet()) {
            GroceryItem item = byId.get(entry.getKey());
            if (item.getQuantityInStock().compareTo(entry.getValue()) < 0) {
                shortages.add(item.getName() + ": need " + entry.getValue()
                        + " " + item.getUnit() + ", have " + item.getQuantityInStock());
            }
        }
        return shortages;
    }

    /** Validates that current stock covers every required grocery item quantity (by id, in stock unit). */
    public void validateStock(Map<Long, BigDecimal> required) {
        List<String> shortages = findShortages(required);
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }
    }

    @Transactional
    public GroceryItem createItem(CreateGroceryItemDto dto) {
        if (groceryItemRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateGroceryItemException(dto.getName());
        }
        Supplier supplier = resolveSupplier(dto.getSupplierId());
        InventoryCategory category = resolveCategory(dto.getCategoryId());
        GroceryItem item = GroceryItem.builder()
                .name(dto.getName())
                .unit(dto.getUnit().toLowerCase())
                .quantityInStock(BigDecimal.ZERO)
                .minThreshold(dto.getMinThreshold())
                .reorderQuantity(dto.getReorderQuantity())
                .defaultSupplier(supplier)
                .category(category)
                .brand(blankToNull(dto.getBrand()))
                .packCost(dto.getPackCost())
                .packQuantity(dto.getPackQuantity())
                .packUnit(lowerBlankToNull(dto.getPackUnit()))
                .minorUnit(lowerBlankToNull(dto.getMinorUnit()))
                .isActive(true)
                .tenantId(1L)
                .build();
        GroceryItem saved = groceryItemRepository.save(item);
        if (saved.isLowStock()) {
            createReplenishmentRequestIfNeeded(saved);
        }
        return saved;
    }

    @Transactional
    public GroceryItem updateItem(Long id, UpdateGroceryItemDto dto) {
        GroceryItem item = getItemById(id);
        if (!item.getName().equalsIgnoreCase(dto.getName())
                && groceryItemRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateGroceryItemException(dto.getName());
        }
        Supplier supplier = resolveSupplier(dto.getSupplierId());
        InventoryCategory category = resolveCategory(dto.getCategoryId());
        item.setName(dto.getName());
        item.setUnit(dto.getUnit().toLowerCase());
        item.setMinThreshold(dto.getMinThreshold());
        item.setReorderQuantity(dto.getReorderQuantity());
        item.setDefaultSupplier(supplier);
        item.setCategory(category);
        item.setBrand(blankToNull(dto.getBrand()));
        item.setPackCost(dto.getPackCost());
        item.setPackQuantity(dto.getPackQuantity());
        item.setPackUnit(lowerBlankToNull(dto.getPackUnit()));
        item.setMinorUnit(lowerBlankToNull(dto.getMinorUnit()));
        GroceryItem saved = groceryItemRepository.save(item);
        if (saved.isLowStock()) {
            createReplenishmentRequestIfNeeded(saved);
        } else {
            cancelPendingReplenishmentRequestsIfAny(saved);
        }
        return saved;
    }

    @Transactional
    public GroceryItem adjustStock(Long id, StockAdjustmentDto dto, Long userId) {
        GroceryItem item = getItemById(id);
        BigDecimal before = item.getQuantityInStock();
        BigDecimal delta = dto.getQuantity();

        BigDecimal after = switch (dto.getMovementType()) {
            case INBOUND, ADJUSTMENT -> before.add(delta);
            case OUTBOUND, WASTAGE -> before.subtract(delta);
        };

        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientStockException(List.of(
                    item.getName() + ": requested " + delta + " but only " + before + " available"
            ));
        }

        item.setQuantityInStock(after);
        groceryItemRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .groceryItem(item)
                .movementType(dto.getMovementType())
                .quantity(delta)
                .stockBefore(before)
                .stockAfter(after)
                .notes(dto.getNotes())
                .createdBy(userId)
                .build();
        stockMovementRepository.save(movement);

        if (item.isLowStock()) {
            createReplenishmentRequestIfNeeded(item);
        } else {
            cancelPendingReplenishmentRequestsIfAny(item);
        }
        menuService.recomputeAllAutoMode();
        return item;
    }

    public void recordMovement(GroceryItem item, MovementType type, BigDecimal qty,
                               String referenceType, Long referenceId, String notes, Long userId) {
        BigDecimal before = item.getQuantityInStock();
        BigDecimal after = switch (type) {
            case INBOUND, ADJUSTMENT -> before.add(qty);
            case OUTBOUND, WASTAGE -> before.subtract(qty);
        };
        item.setQuantityInStock(after);
        groceryItemRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .groceryItem(item)
                .movementType(type)
                .quantity(qty)
                .stockBefore(before)
                .stockAfter(after)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .notes(notes)
                .createdBy(userId)
                .build();
        stockMovementRepository.save(movement);

        if (item.isLowStock()) {
            createReplenishmentRequestIfNeeded(item);
        } else {
            cancelPendingReplenishmentRequestsIfAny(item);
        }
    }

    @Transactional
    public void deductStock(Long groceryItemId, BigDecimal qty, Long billId, Long userId) {
        GroceryItem item = groceryItemRepository.findById(groceryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Grocery item not found with id: " + groceryItemId));
        BigDecimal before = item.getQuantityInStock();
        if (before.compareTo(qty) < 0) {
            throw new InsufficientStockException(List.of(
                    item.getName() + ": need " + qty + " " + item.getUnit() + ", have " + before));
        }
        BigDecimal after = before.subtract(qty);
        item.setQuantityInStock(after);
        groceryItemRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .groceryItem(item)
                .movementType(MovementType.OUTBOUND)
                .quantity(qty)
                .stockBefore(before)
                .stockAfter(after)
                .referenceType("BILL")
                .referenceId(billId)
                .createdBy(userId)
                .build();
        stockMovementRepository.save(movement);

        if (item.isLowStock()) {
            createReplenishmentRequestIfNeeded(item);
        }
    }

    public Page<StockMovement> getMovementsForItem(Long itemId, Pageable pageable) {
        return stockMovementRepository.findByGroceryItemId(itemId, pageable);
    }

    @Transactional
    public void backfillReplenishmentRequests() {
        groceryItemRepository.findLowStockItems()
                .forEach(this::createReplenishmentRequestIfNeeded);
    }

    private void cancelPendingReplenishmentRequestsIfAny(GroceryItem item) {
        List<ReplenishmentRequest> pending = replenishmentRequestRepository
                .findByGroceryItemIdAndStatus(item.getId(), ReplenishmentStatus.PENDING);
        if (pending.isEmpty()) return;
        pending.forEach(r -> r.setStatus(ReplenishmentStatus.CANCELLED));
        replenishmentRequestRepository.saveAll(pending);
    }

    private void createReplenishmentRequestIfNeeded(GroceryItem item) {
        boolean hasPendingRequest = replenishmentRequestRepository.existsByGroceryItemIdAndStatus(
                item.getId(), ReplenishmentStatus.PENDING);
        if (hasPendingRequest) return;
        boolean hasActiveOrder = purchaseOrderRepository.existsActiveOrderForGroceryItem(
                item.getId(), ACTIVE_PO_STATUSES);
        if (hasActiveOrder) return;
        ReplenishmentRequest request = ReplenishmentRequest.builder()
                .groceryItem(item)
                .status(ReplenishmentStatus.PENDING)
                .requestedQty(item.getReorderQuantity())
                .tenantId(1L)
                .build();
        replenishmentRequestRepository.save(request);
    }

    @Transactional
    public void deleteItem(Long id) {
        GroceryItem item = getItemById(id);
        if (stockMovementRepository.existsByGroceryItemId(id)
                || replenishmentRequestRepository.existsByGroceryItemId(id)
                || purchaseOrderRepository.existsPurchaseOrderItemForGroceryItem(id)
                || dishRepository.existsByIngredientsGroceryItemId(id)
                || comboRepository.existsByIngredientsGroceryItemId(id)
                || preparedItemRepository.existsByIngredientsGroceryItemId(id)) {
            throw new GroceryItemInUseException(item.getName());
        }
        groceryItemRepository.delete(item);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String lowerBlankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim().toLowerCase();
    }

    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
    }

    private InventoryCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return inventoryCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory category not found with id: " + categoryId));
    }
}
