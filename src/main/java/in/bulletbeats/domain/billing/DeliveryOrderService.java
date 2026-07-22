package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.dto.CategoryWithItemsDto;
import in.bulletbeats.domain.billing.dto.DeliveryMenuDto;
import in.bulletbeats.domain.billing.dto.DeliveryStartResult;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.BillItem;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.billing.service.BillNumberService;
import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.service.CustomerService;
import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.entity.ComboIngredient;
import in.bulletbeats.domain.menu.entity.DishIngredient;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.shared.enums.ActorType;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.OrderType;
import in.bulletbeats.domain.shared.exception.BillNotEditableException;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryOrderService {

    private static final Long SYSTEM_USER_ID = 0L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BillRepository billRepository;
    private final BillNumberService billNumberService;
    private final CustomerService customerService;
    private final MenuService menuService;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;
    private final GroceryItemRepository groceryItemRepository;
    private final ActivityLogService activityLogService;
    private final AppConfigService appConfigService;

    @Transactional
    public DeliveryStartResult startOrder(String phone, String name, String address) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required for delivery orders");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required for delivery orders");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Delivery address is required");
        }

        boolean isReturningCustomer = customerService.existsByPhone(phone.trim());
        Customer customer = customerService.findOrCreateByPhone(phone.trim(), name.trim(), SYSTEM_USER_ID);

        String billNumber = billNumberService.generateBillNumber();
        Bill bill = Bill.builder()
                .billNumber(billNumber)
                .orderType(OrderType.DIRECT_DELIVERY)
                .customer(customer)
                .deliveryAddress(address.trim())
                .status(BillStatus.DRAFT)
                .build();
        bill = billRepository.save(bill);

        activityLogService.log(bill.getId(), ActorType.SYSTEM, "System",
                "Bill created via direct delivery order");

        return new DeliveryStartResult(bill, customer, isReturningCustomer);
    }

    @Transactional(readOnly = true)
    public DeliveryMenuDto getMenuForOrder(Long billId) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        List<Category> categories = categoryService.getAllActive();
        List<MenuItem> allAvailable = menuService.getAllAvailableItems();

        Map<Long, List<MenuItem>> byCategory = allAvailable.stream()
                .filter(item -> item.getCategory() != null)
                .collect(Collectors.groupingBy(item -> item.getCategory().getId()));

        List<CategoryWithItemsDto> grouped = categories.stream()
                .map(cat -> new CategoryWithItemsDto(cat, byCategory.getOrDefault(cat.getId(), List.of())))
                .filter(dto -> !dto.items().isEmpty())
                .collect(Collectors.toList());

        String customerName = bill.getCustomer() != null ? bill.getCustomer().getName() : null;

        return new DeliveryMenuDto(billId, bill.getDeliveryAddress(), customerName, grouped);
    }

    @Transactional
    public Bill addItem(Long billId, Long menuItemId, int quantity, String customerName) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BillNotEditableException("Order " + bill.getBillNumber() + " has already been placed and can't be changed");
        }

        MenuItem menuItem = menuService.getItemById(menuItemId);
        if (!menuItem.isAvailable()) {
            throw new ResourceNotFoundException("Menu item '" + menuItem.getName() + "' is not available");
        }

        Optional<BillItem> existing = bill.getItems().stream()
                .filter(bi -> bi.getMenuItem().getId().equals(menuItemId))
                .findFirst();

        int currentQty = existing.map(BillItem::getQuantity).orElse(0);
        int newTotalQty = currentQty + quantity;

        if (!isForceAvailable(menuItem)) {
            validateStock(aggregateIngredientsForItem(menuItem, newTotalQty));
        }

        if (existing.isPresent()) {
            BillItem item = existing.get();
            item.setQuantity(newTotalQty);
            item.recalculate();
        } else {
            BillItem newItem = BillItem.builder()
                    .bill(bill)
                    .menuItem(menuItem)
                    .itemName(menuItem.getName())
                    .unitPrice(menuItem.getPrice())
                    .quantity(quantity)
                    .lineTotal(menuItem.getPrice().multiply(BigDecimal.valueOf(quantity)))
                    .build();
            bill.getItems().add(newItem);
        }

        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String actor = customerName != null ? customerName : "Delivery customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] " + menuItem.getName() + " x" + quantity + " added via delivery order by " + actor);

        return saved;
    }

    @Transactional
    public Bill removeItem(Long billId, Long billItemId, String customerName) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BillNotEditableException("Order " + bill.getBillNumber() + " has already been placed and can't be changed");
        }

        BillItem billItem = bill.getItems().stream()
                .filter(bi -> bi.getId().equals(billItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in bill"));

        String itemName = billItem.getItemName();
        int qty = billItem.getQuantity();

        bill.getItems().remove(billItem);
        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String actor = customerName != null ? customerName : "Delivery customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] " + itemName + " x" + qty + " removed via delivery order by " + actor);

        return saved;
    }

    @Transactional
    public Bill updateItemQuantity(Long billId, Long billItemId, int newQty, String customerName) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BillNotEditableException("Order " + bill.getBillNumber() + " has already been placed and can't be changed");
        }

        if (newQty <= 0) {
            return removeItem(billId, billItemId, customerName);
        }

        BillItem item = bill.getItems().stream()
                .filter(bi -> bi.getId().equals(billItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in bill"));

        if (!isForceAvailable(item.getMenuItem()) && newQty > item.getQuantity()) {
            validateStock(aggregateIngredientsForItem(item.getMenuItem(), newQty));
        }

        String itemName = item.getItemName();
        item.setQuantity(newQty);
        item.recalculate();
        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String actor = customerName != null ? customerName : "Delivery customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] " + itemName + " qty updated to " + newQty + " via delivery order by " + actor);

        return saved;
    }

    @Transactional
    public Bill confirmOrder(Long billId, String customerName) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BillNotEditableException("Order " + bill.getBillNumber() + " has already been placed");
        }
        if (bill.getItems().isEmpty()) {
            throw new BillNotEditableException("Cannot place an empty order");
        }

        Map<Long, BigDecimal> required = aggregateAllIngredients(bill);
        validateStock(required);
        for (Map.Entry<Long, BigDecimal> e : required.entrySet()) {
            inventoryService.deductStock(e.getKey(), e.getValue(), billId, SYSTEM_USER_ID);
        }
        bill.setStatus(BillStatus.CONFIRMED);
        bill.setConfirmedAt(java.time.LocalDateTime.now());
        menuService.recomputeAllAutoMode();

        String actor = customerName != null ? customerName : "Delivery customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] Order placed via direct delivery by " + actor);

        return billRepository.save(bill);
    }

    @Transactional(readOnly = true)
    public Bill getOrder(Long billId) {
        return billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private Map<Long, BigDecimal> aggregateIngredientsForItem(MenuItem menuItem, int qty) {
        Map<Long, BigDecimal> required = new HashMap<>();
        if (menuItem.getDish() != null) {
            for (DishIngredient ing : menuItem.getDish().getIngredients()) {
                required.merge(ing.getGroceryItem().getId(),
                        ing.getQuantityRequired().multiply(BigDecimal.valueOf(qty)),
                        BigDecimal::add);
            }
        } else if (menuItem.getCombo() != null) {
            for (ComboIngredient ing : menuItem.getCombo().getIngredients()) {
                required.merge(ing.getGroceryItem().getId(),
                        ing.getQuantityRequired().multiply(BigDecimal.valueOf(qty)),
                        BigDecimal::add);
            }
        }
        return required;
    }

    private Map<Long, BigDecimal> aggregateAllIngredients(Bill bill) {
        Map<Long, BigDecimal> required = new HashMap<>();
        for (BillItem item : bill.getItems()) {
            if (!isForceAvailable(item.getMenuItem())) {
                aggregateIngredientsForItem(item.getMenuItem(), item.getQuantity())
                        .forEach((k, v) -> required.merge(k, v, BigDecimal::add));
            }
        }
        return required;
    }

    private boolean isForceAvailable(MenuItem item) {
        return Boolean.TRUE.equals(item.getAvailabilityOverride());
    }

    private void validateStock(Map<Long, BigDecimal> required) {
        if (required.isEmpty()) return;
        Map<Long, GroceryItem> byId = groceryItemRepository.findAllById(required.keySet())
                .stream().collect(Collectors.toMap(GroceryItem::getId, g -> g));
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : required.entrySet()) {
            GroceryItem item = byId.get(e.getKey());
            if (item.getQuantityInStock().compareTo(e.getValue()) < 0) {
                shortages.add(item.getName() + ": need " + e.getValue()
                        + " " + item.getUnit() + ", have " + item.getQuantityInStock());
            }
        }
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }
    }

    private void recalculateTotals(Bill bill) {
        BigDecimal subtotal = bill.getItems().stream()
                .map(BillItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (bill.getDiscountType() != null && bill.getDiscountValue() != null) {
            discountAmount = switch (bill.getDiscountType()) {
                case FIXED -> bill.getDiscountValue().min(subtotal);
                case PERCENTAGE -> subtotal.multiply(bill.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            };
        }

        BigDecimal gstRate = appConfigService.getDecimal("gst.rate", new BigDecimal("18.00"));
        boolean gstInclusive = appConfigService.getBoolean("gst.inclusive", false);

        BigDecimal taxableAmount;
        BigDecimal gstAmount;
        BigDecimal totalAmount;

        if (gstInclusive) {
            BigDecimal grossAfterDiscount = subtotal.subtract(discountAmount);
            gstAmount = grossAfterDiscount.multiply(gstRate)
                    .divide(BigDecimal.valueOf(100).add(gstRate), 2, RoundingMode.HALF_UP);
            taxableAmount = grossAfterDiscount.subtract(gstAmount);
            totalAmount = grossAfterDiscount;
        } else {
            taxableAmount = subtotal.subtract(discountAmount);
            gstAmount = taxableAmount.multiply(gstRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            totalAmount = taxableAmount.add(gstAmount);
        }

        bill.setSubtotal(subtotal);
        bill.setDiscountAmount(discountAmount);
        bill.setTaxableAmount(taxableAmount);
        bill.setGstRate(gstRate);
        bill.setGstInclusive(gstInclusive);
        bill.setGstAmount(gstAmount);
        bill.setTotalAmount(totalAmount);
    }
}
