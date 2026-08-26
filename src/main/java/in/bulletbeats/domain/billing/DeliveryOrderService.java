package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.dto.CategoryWithItemsDto;
import in.bulletbeats.domain.billing.dto.DeliveryHoursStatus;
import in.bulletbeats.domain.billing.dto.DeliveryMenuDto;
import in.bulletbeats.domain.billing.dto.DeliveryStartResult;
import in.bulletbeats.domain.billing.dto.SubcategoryWithItemsDto;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.BillItem;
import in.bulletbeats.domain.billing.repository.BillItemRepository;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.billing.service.BillNumberService;
import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.service.CustomerService;
import in.bulletbeats.domain.menu.dto.CategoryNode;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.IngredientAggregationService;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.notification.DeliveryOrderNotificationData;
import in.bulletbeats.domain.notification.NotificationChannel;
import in.bulletbeats.domain.notification.NotificationService;
import in.bulletbeats.domain.notification.WhatsappTemplate;
import in.bulletbeats.domain.shared.enums.ActorType;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.OrderType;
import in.bulletbeats.domain.shared.exception.BillNotEditableException;
import in.bulletbeats.domain.shared.exception.DeliveryClosedException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import in.bulletbeats.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter DISPLAY_TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");
    private static final long DUPLICATE_ORDER_WINDOW_SECONDS = 30;

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final BillNumberService billNumberService;
    private final CustomerService customerService;
    private final MenuService menuService;
    private final CategoryService categoryService;
    private final IngredientAggregationService ingredientAggregationService;
    private final ActivityLogService activityLogService;
    private final AppConfigService appConfigService;
    private final NotificationService notificationService;
    private final UserService userService;

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

        // Guards against duplicate bills from double-tapped/double-submitted start requests
        // (e.g. the landing page's welcome-back auto-submit racing a manual "Continue" click).
        Optional<Bill> recentDraft = billRepository
                .findFirstByCustomerIdAndOrderTypeAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        customer.getId(), OrderType.DIRECT_DELIVERY, BillStatus.DRAFT,
                        java.time.LocalDateTime.now().minusSeconds(DUPLICATE_ORDER_WINDOW_SECONDS));
        if (recentDraft.isPresent()) {
            return new DeliveryStartResult(recentDraft.get(), customer, isReturningCustomer);
        }

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

        List<CategoryWithItemsDto> grouped = getGroupedMenuItems();

        String customerName = bill.getCustomer() != null ? bill.getCustomer().getName() : null;

        return new DeliveryMenuDto(billId, bill.getDeliveryAddress(), customerName, grouped);
    }

    @Transactional(readOnly = true)
    public List<CategoryWithItemsDto> getGroupedMenuItems() {
        List<CategoryNode> tree = categoryService.getActiveCategoryTree();
        List<MenuItem> allAvailable = menuService.getAllAvailableItems();

        Map<Long, List<MenuItem>> byCategory = allAvailable.stream()
                .filter(item -> item.getCategory() != null)
                .collect(Collectors.groupingBy(item -> item.getCategory().getId()));

        return tree.stream()
                .map(node -> {
                    List<MenuItem> direct = byCategory.getOrDefault(node.category().getId(), List.of());
                    List<SubcategoryWithItemsDto> subs = node.subcategories().stream()
                            .map(sub -> new SubcategoryWithItemsDto(
                                    sub, byCategory.getOrDefault(sub.getId(), List.of())))
                            .filter(s -> !s.items().isEmpty())
                            .toList();
                    return new CategoryWithItemsDto(node.category(), direct, subs);
                })
                .filter(dto -> !dto.items().isEmpty() || !dto.subcategories().isEmpty())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryHoursStatus getHoursStatus() {
        if (!appConfigService.getBoolean("delivery.hours.enabled", false)) {
            return new DeliveryHoursStatus(false, null);
        }
        LocalTime start = appConfigService.getTime("delivery.closed.start", LocalTime.MIDNIGHT);
        LocalTime end = appConfigService.getTime("delivery.closed.end", LocalTime.MIDNIGHT);
        if (start.equals(end)) {
            return new DeliveryHoursStatus(false, null);
        }
        LocalTime now = LocalTime.now();
        boolean closed = start.isBefore(end)
                ? !now.isBefore(start) && now.isBefore(end)
                : !now.isBefore(start) || now.isBefore(end);
        return new DeliveryHoursStatus(closed, closed ? end.format(DISPLAY_TIME_FMT) : null);
    }

    private void assertOpenForOrdering() {
        DeliveryHoursStatus hours = getHoursStatus();
        if (hours.closed()) {
            throw new DeliveryClosedException(
                    "We're closed for delivery orders right now. Ordering opens again at " + hours.reopensAtLabel() + ".");
        }
    }

    @Transactional
    public Bill addItem(Long billId, Long menuItemId, int quantity, String customerName, String note) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (bill.getStatus().isTerminal()) {
            throw new BillNotEditableException("Order " + bill.getBillNumber() + " has already been placed and can't be changed");
        }
        assertOpenForOrdering();
        boolean wasConfirmed = bill.getStatus() == BillStatus.CONFIRMED;

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
            if (wasConfirmed) {
                // Stock for pre-existing quantity was already deducted at confirmation time —
                // only the newly added quantity needs to be deducted now.
                deductStockForMenuItem(menuItem, quantity, billId);
            } else {
                validateStock(aggregateIngredientsForItem(menuItem, newTotalQty));
            }
        }

        String normalizedNote = normalizeNote(note);

        if (existing.isPresent()) {
            BillItem item = existing.get();
            item.setQuantity(newTotalQty);
            item.recalculate();
            if (normalizedNote != null) {
                item.setNote(normalizedNote);
            }
        } else {
            BillItem newItem = BillItem.builder()
                    .bill(bill)
                    .menuItem(menuItem)
                    .itemName(menuItem.getName())
                    .unitPrice(menuItem.getPrice())
                    .quantity(quantity)
                    .lineTotal(menuItem.getPrice().multiply(BigDecimal.valueOf(quantity)))
                    .note(normalizedNote)
                    .build();
            bill.getItems().add(newItem);
        }

        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String actor = customerName != null ? customerName : "Delivery customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] " + menuItem.getName() + " x" + quantity + " added via delivery order by " + actor);

        if (wasConfirmed) {
            notifyStaffOfOrder(saved);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> getSavedNotesForCustomer(Long customerId) {
        if (customerId == null) {
            return Map.of();
        }
        return billItemRepository.findLatestNotesByCustomer(customerId).stream()
                .collect(Collectors.toMap(
                        BillItemRepository.MenuItemNoteRow::getMenuItemId,
                        BillItemRepository.MenuItemNoteRow::getNote));
    }

    private String normalizeNote(String note) {
        if (note == null) return null;
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

        if (bill.getStatus().isTerminal()) {
            throw new BillNotEditableException("Order " + bill.getBillNumber() + " has already been placed and can't be changed");
        }
        boolean wasConfirmed = bill.getStatus() == BillStatus.CONFIRMED;

        BillItem item = bill.getItems().stream()
                .filter(bi -> bi.getId().equals(billItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in bill"));

        if (wasConfirmed && newQty <= item.getQuantity()) {
            throw new BillNotEditableException("Order " + bill.getBillNumber()
                    + " has already been placed — items can only be added, not reduced or removed.");
        }

        if (newQty > item.getQuantity()) {
            assertOpenForOrdering();
        }

        if (newQty <= 0) {
            return removeItem(billId, billItemId, customerName);
        }

        int delta = newQty - item.getQuantity();

        if (!isForceAvailable(item.getMenuItem())) {
            if (wasConfirmed) {
                deductStockForMenuItem(item.getMenuItem(), delta, billId);
            } else if (newQty > item.getQuantity()) {
                validateStock(aggregateIngredientsForItem(item.getMenuItem(), newQty));
            }
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

        if (wasConfirmed) {
            notifyStaffOfOrder(saved);
        }

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

        IngredientAggregationService.Requirements required = aggregateAllIngredients(bill);
        ingredientAggregationService.validateStock(required);
        ingredientAggregationService.deduct(required, billId, SYSTEM_USER_ID);
        bill.setStatus(BillStatus.CONFIRMED);
        bill.setConfirmedAt(java.time.LocalDateTime.now());
        menuService.recomputeAllAutoMode();
        notifyStaffOfOrder(bill);

        String actor = customerName != null ? customerName : "Delivery customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] Order placed via direct delivery by " + actor);

        return billRepository.save(bill);
    }

    private void notifyStaffOfOrder(Bill bill) {
        List<String> staffPhones = userService.getActiveStaffPhones();
        if (staffPhones.isEmpty()) {
            return;
        }

        String customerName = bill.getCustomer() != null ? bill.getCustomer().getName() : "N/A";
        String customerPhone = bill.getCustomer() != null ? bill.getCustomer().getPhone() : "N/A";
        String address = bill.getDeliveryAddress() != null
                ? bill.getDeliveryAddress().replaceAll("\\s*\\r?\\n\\s*", ", ").trim()
                : "N/A";
        String itemsSummary = bill.getItems().stream()
                .map(i -> i.getQuantity() + "x " + i.getItemName())
                .collect(Collectors.joining(", "));
        String total = "₹" + bill.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

        String formattedText = "New direct delivery order — Bill #" + bill.getBillNumber()
                + ", Customer " + customerName + " (" + customerPhone + ")"
                + ", Address: " + address + ", Items: " + itemsSummary + ", Total: " + total;

        for (String staffPhone : staffPhones) {
            notificationService.send(WhatsappTemplate.ORDER_RECEIVED_DELIVERY, new DeliveryOrderNotificationData(
                    staffPhone, NotificationChannel.WHATSAPP, bill.getBillNumber(),
                    customerName, customerPhone, address, itemsSummary, total, formattedText));
        }
    }

    @Transactional(readOnly = true)
    public Bill getOrder(Long billId) {
        return billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void deductStockForMenuItem(MenuItem menuItem, int qty, Long billId) {
        IngredientAggregationService.Requirements required = aggregateIngredientsForItem(menuItem, qty);
        ingredientAggregationService.validateStock(required);
        ingredientAggregationService.deduct(required, billId, SYSTEM_USER_ID);
    }

    private IngredientAggregationService.Requirements aggregateIngredientsForItem(MenuItem menuItem, int qty) {
        return ingredientAggregationService.aggregateForItem(menuItem, qty);
    }

    private IngredientAggregationService.Requirements aggregateAllIngredients(Bill bill) {
        IngredientAggregationService.Requirements required = new IngredientAggregationService.Requirements();
        for (BillItem item : bill.getItems()) {
            if (!isForceAvailable(item.getMenuItem())) {
                aggregateIngredientsForItem(item.getMenuItem(), item.getQuantity()).mergeInto(required);
            }
        }
        return required;
    }

    private boolean isForceAvailable(MenuItem item) {
        return Boolean.TRUE.equals(item.getAvailabilityOverride());
    }

    private void validateStock(IngredientAggregationService.Requirements required) {
        ingredientAggregationService.validateStock(required);
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
        BigDecimal deliveryFee = appConfigService.getDecimal("delivery.fee", BigDecimal.ZERO);

        BigDecimal taxableAmount;
        BigDecimal gstAmount;
        BigDecimal totalAmount;

        if (gstInclusive) {
            BigDecimal grossAfterDiscount = subtotal.subtract(discountAmount);
            gstAmount = grossAfterDiscount.multiply(gstRate)
                    .divide(BigDecimal.valueOf(100).add(gstRate), 2, RoundingMode.HALF_UP);
            taxableAmount = grossAfterDiscount.subtract(gstAmount);
            totalAmount = grossAfterDiscount.add(deliveryFee);
        } else {
            taxableAmount = subtotal.subtract(discountAmount);
            gstAmount = taxableAmount.multiply(gstRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            totalAmount = taxableAmount.add(gstAmount).add(deliveryFee);
        }

        bill.setSubtotal(subtotal);
        bill.setDiscountAmount(discountAmount);
        bill.setTaxableAmount(taxableAmount);
        bill.setGstRate(gstRate);
        bill.setGstInclusive(gstInclusive);
        bill.setGstAmount(gstAmount);
        bill.setDeliveryFee(deliveryFee);
        bill.setTotalAmount(totalAmount);
    }
}
