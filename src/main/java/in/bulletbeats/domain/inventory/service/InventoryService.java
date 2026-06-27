package in.bulletbeats.domain.inventory.service;

import in.bulletbeats.domain.inventory.dto.CreateGroceryItemDto;
import in.bulletbeats.domain.inventory.dto.StockAdjustmentDto;
import in.bulletbeats.domain.inventory.dto.UpdateGroceryItemDto;
import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.ReplenishmentRequest;
import in.bulletbeats.domain.inventory.entity.StockMovement;
import in.bulletbeats.domain.inventory.entity.Supplier;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.repository.PurchaseOrderRepository;
import in.bulletbeats.domain.inventory.repository.ReplenishmentRequestRepository;
import in.bulletbeats.domain.inventory.repository.StockMovementRepository;
import in.bulletbeats.domain.inventory.repository.SupplierRepository;
import in.bulletbeats.domain.shared.enums.MovementType;
import in.bulletbeats.domain.shared.enums.PurchaseOrderStatus;
import in.bulletbeats.domain.shared.enums.ReplenishmentStatus;
import in.bulletbeats.domain.shared.exception.DuplicateGroceryItemException;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
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
import java.util.Comparator;
import java.util.List;
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

    public List<String> suggestUnits(String prefix) {
        return groceryItemRepository.findDistinctUnitsByPrefix(prefix == null ? "" : prefix.toLowerCase());
    }

    @Transactional
    public GroceryItem createItem(CreateGroceryItemDto dto) {
        if (groceryItemRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateGroceryItemException(dto.getName());
        }
        Supplier supplier = resolveSupplier(dto.getSupplierId());
        GroceryItem item = GroceryItem.builder()
                .name(dto.getName())
                .unit(dto.getUnit().toLowerCase())
                .quantityInStock(BigDecimal.ZERO)
                .minThreshold(dto.getMinThreshold())
                .reorderQuantity(dto.getReorderQuantity())
                .defaultSupplier(supplier)
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
        item.setName(dto.getName());
        item.setUnit(dto.getUnit().toLowerCase());
        item.setMinThreshold(dto.getMinThreshold());
        item.setReorderQuantity(dto.getReorderQuantity());
        item.setDefaultSupplier(supplier);
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

    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
    }
}
