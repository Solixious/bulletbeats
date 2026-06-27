package in.bulletbeats.domain.inventory.service;

import in.bulletbeats.domain.inventory.dto.ReceiveItemDto;
import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.PurchaseOrder;
import in.bulletbeats.domain.inventory.entity.PurchaseOrderItem;
import in.bulletbeats.domain.inventory.entity.ReplenishmentRequest;
import in.bulletbeats.domain.inventory.entity.Supplier;
import in.bulletbeats.domain.inventory.repository.PurchaseOrderRepository;
import in.bulletbeats.domain.shared.enums.MovementType;
import in.bulletbeats.domain.shared.enums.PurchaseOrderStatus;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final AvailabilityService availabilityService;

    @Lazy
    @Autowired
    private MenuService menuService;

    @Value("${app.cafe-name:The Bullet Café}")
    private String cafeName;

    public List<PurchaseOrder> getAllOrders() {
        return purchaseOrderRepository.findAllWithSupplierOrderByCreatedAtDesc();
    }

    public PurchaseOrder getById(Long id) {
        return purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
    }

    @Transactional
    public void createOrAppendPO(ReplenishmentRequest request, Supplier supplier) {
        GroceryItem groceryItem = request.getGroceryItem();
        if (supplier == null) {
            return;
        }

        PurchaseOrder po = purchaseOrderRepository
                .findBySupplierIdAndStatus(supplier.getId(), PurchaseOrderStatus.PENDING)
                .orElseGet(() -> purchaseOrderRepository.save(PurchaseOrder.builder()
                        .supplier(supplier)
                        .status(PurchaseOrderStatus.PENDING)
                        .tenantId(1L)
                        .build()));

        PurchaseOrderItem item = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .groceryItem(groceryItem)
                .quantityOrdered(request.getRequestedQty())
                .quantityReceived(BigDecimal.ZERO)
                .build();
        po.getItems().add(item);

        generateWhatsappText(po);
        purchaseOrderRepository.save(po);
    }

    @Transactional
    public void createManualPOs(List<Long> groceryItemIds, List<Long> supplierIds, List<BigDecimal> qtys) {
        if (groceryItemIds == null || groceryItemIds.isEmpty()) return;
        int size = Math.min(Math.min(groceryItemIds.size(), supplierIds.size()), qtys.size());
        for (int i = 0; i < size; i++) {
            Long giId = groceryItemIds.get(i);
            Long supId = supplierIds.get(i);
            BigDecimal qty = qtys.get(i);
            if (giId == null || giId == 0 || supId == null || supId == 0
                    || qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) continue;

            GroceryItem groceryItem = inventoryService.getItemById(giId);
            Supplier supplier = supplierService.getById(supId);

            PurchaseOrder po = purchaseOrderRepository
                    .findBySupplierIdAndStatus(supId, PurchaseOrderStatus.PENDING)
                    .orElseGet(() -> purchaseOrderRepository.save(PurchaseOrder.builder()
                            .supplier(supplier)
                            .status(PurchaseOrderStatus.PENDING)
                            .tenantId(1L)
                            .build()));

            po.getItems().add(PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .groceryItem(groceryItem)
                    .quantityOrdered(qty)
                    .quantityReceived(BigDecimal.ZERO)
                    .build());
            generateWhatsappText(po);
            purchaseOrderRepository.save(po);
        }
    }

    @Transactional
    public void cancelOrder(Long id) {
        PurchaseOrder po = getById(id);
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder updateItemQty(Long poId, Long itemId, BigDecimal qty) {
        PurchaseOrder po = getById(poId);
        po.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in order: " + itemId))
                .setQuantityOrdered(qty);
        generateWhatsappText(po);
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder removeItem(Long poId, Long itemId) {
        PurchaseOrder po = getById(poId);
        po.getItems().removeIf(i -> i.getId().equals(itemId));
        if (po.getItems().isEmpty()) {
            po.setStatus(PurchaseOrderStatus.CANCELLED);
        }
        generateWhatsappText(po);
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public void markAsOrdered(Long id, BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero.");
        }
        PurchaseOrder po = getById(id);
        po.setStatus(PurchaseOrderStatus.ORDERED);
        po.setOrderedAt(LocalDateTime.now());
        po.setTotalAmount(totalAmount);
        purchaseOrderRepository.save(po);
    }

    @Transactional
    public void receivePO(Long id, List<ReceiveItemDto> receiveDtos, Long userId) {
        PurchaseOrder po = getById(id);

        Map<Long, PurchaseOrderItem> itemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, Function.identity()));

        for (ReceiveItemDto dto : receiveDtos) {
            PurchaseOrderItem poItem = itemMap.get(dto.getPurchaseOrderItemId());
            if (poItem == null) {
                continue;
            }
            poItem.setQuantityReceived(dto.getQuantityReceived());
            inventoryService.recordMovement(
                    poItem.getGroceryItem(),
                    MovementType.INBOUND,
                    dto.getQuantityReceived(),
                    "PURCHASE_ORDER",
                    po.getId(),
                    null,
                    userId
            );
        }

        boolean allReceived = po.getItems().stream()
                .allMatch(i -> i.getQuantityReceived().compareTo(i.getQuantityOrdered()) >= 0);
        if (allReceived) {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
        }

        purchaseOrderRepository.save(po);
        availabilityService.recomputeAll();
        menuService.recomputeAllAutoMode();
    }

    private void generateWhatsappText(PurchaseOrder po) {
        Supplier supplier = po.getSupplier();
        StringBuilder sb = new StringBuilder();
        sb.append("📦 Purchase Order — ").append(cafeName).append("\n");
        sb.append("Date: ").append(LocalDate.now()).append("\n\n");
        sb.append("Supplier: ").append(supplier.getName()).append("\n");
        if (supplier.getPhone() != null) {
            sb.append("Phone: ").append(supplier.getPhone()).append("\n");
        }
        sb.append("---\n");
        for (PurchaseOrderItem item : po.getItems()) {
            GroceryItem gi = item.getGroceryItem();
            sb.append("- ").append(gi.getName())
              .append(": ").append(item.getQuantityOrdered().stripTrailingZeros().toPlainString())
              .append(" ").append(gi.getUnit()).append("\n");
        }
        sb.append("\nPlease confirm delivery date.\n");
        sb.append("— ").append(cafeName);
        po.setWhatsappText(sb.toString());
    }
}
