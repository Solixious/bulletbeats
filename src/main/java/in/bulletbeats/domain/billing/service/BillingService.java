package in.bulletbeats.domain.billing.service;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.ActivityLogService;
import in.bulletbeats.domain.notification.BillNotificationData;
import in.bulletbeats.domain.notification.NotificationChannel;
import in.bulletbeats.domain.notification.NotificationService;
import in.bulletbeats.domain.notification.WhatsappTemplate;
import in.bulletbeats.domain.billing.dto.AddBillItemDto;
import in.bulletbeats.domain.billing.dto.ApplyDiscountDto;
import in.bulletbeats.domain.billing.dto.CategoryWithItemsDto;
import in.bulletbeats.domain.billing.dto.CreateBillDto;
import in.bulletbeats.domain.billing.dto.PayBillDto;
import in.bulletbeats.domain.billing.dto.SubcategoryWithItemsDto;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.BillItem;
import in.bulletbeats.domain.billing.entity.Payment;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.billing.repository.PaymentRepository;
import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.service.CustomerService;
import in.bulletbeats.domain.crm.service.LoyaltyService;
import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.menu.dto.CategoryNode;
import in.bulletbeats.domain.menu.entity.ComboIngredient;
import in.bulletbeats.domain.menu.entity.DishIngredient;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.shared.enums.ActorType;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.DiscountType;
import in.bulletbeats.domain.shared.enums.MovementType;
import in.bulletbeats.domain.shared.enums.OrderType;
import in.bulletbeats.domain.shared.exception.BillNotEditableException;
import in.bulletbeats.domain.shared.exception.EmptyBillException;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import in.bulletbeats.domain.shared.exception.StudentDiscountException;
import in.bulletbeats.domain.shared.exception.TableNotActiveException;
import in.bulletbeats.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Transactional(readOnly = true)
public class BillingService {

    private static final List<BillStatus> TERMINAL = List.of(BillStatus.PAID, BillStatus.CANCELLED);
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final CafeTableService cafeTableService;
    private final BillNumberService billNumberService;
    private final CustomerService customerService;
    private final LoyaltyService loyaltyService;
    private final MenuService menuService;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;
    private final GroceryItemRepository groceryItemRepository;
    private final AppConfigService appConfigService;
    private final ActivityLogService activityLogService;
    private final UserService userService;
    private final NotificationService notificationService;

    public List<Bill> getActiveBills() {
        return billRepository.findActiveBills(TERMINAL);
    }

    public long countActiveBills() {
        return billRepository.countActiveBills(TERMINAL);
    }

    public Map<Long, Long> getActiveBillCountByTable() {
        return billRepository.countActiveByTableId(TERMINAL).stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));
    }

    public Bill getBillById(Long id) {
        return billRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id: " + id));
    }

    /**
     * Active menu items (available and unavailable — staff can see both) grouped by category
     * and subcategory in tree order, for the "All" view of the billing menu picker.
     */
    public List<CategoryWithItemsDto> getGroupedMenuItems() {
        List<CategoryNode> tree = categoryService.getActiveCategoryTree();
        List<MenuItem> allActive = menuService.getAllItems();

        Map<Long, List<MenuItem>> byCategory = allActive.stream()
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

    public List<Bill> getBillsForTable(Long tableId) {
        return billRepository.findByCafeTableIdAndStatusNotInOrderByCreatedAtDesc(tableId, TERMINAL);
    }

    public Page<Bill> getBillsForCustomer(Long customerId, Pageable pageable) {
        return billRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }

    public List<Bill> getRecentBillsForCustomer(Long customerId) {
        return billRepository.findTop5ByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Page<Bill> getHistory(LocalDate from, LocalDate to, String phone, Pageable pageable) {
        final String p = (phone != null && !phone.isBlank()) ? phone.trim() : null;
        final LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        final LocalDateTime toDt = to != null ? to.plusDays(1).atStartOfDay() : null;

        Specification<Bill> spec = (root, query, cb) -> {
            boolean isCount = Long.class.equals(query.getResultType());
            List<Predicate> preds = new ArrayList<>();

            if (!isCount) {
                root.fetch("cafeTable", JoinType.LEFT);
                query.distinct(true);
            }

            if (p != null) {
                // Cast fetch to Join so it can serve both fetching and filtering
                Join<?, ?> customerJoin = !isCount
                        ? (Join<?, ?>) root.fetch("customer", JoinType.LEFT)
                        : root.join("customer", JoinType.LEFT);
                preds.add(cb.equal(customerJoin.get("phone"), p));
            } else if (!isCount) {
                root.fetch("customer", JoinType.LEFT);
            }

            if (fromDt != null) preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDt));
            if (toDt != null)   preds.add(cb.lessThan(root.get("createdAt"), toDt));

            return preds.isEmpty() ? cb.conjunction() : cb.and(preds.toArray(new Predicate[0]));
        };

        return billRepository.findAll(spec, pageable);
    }

    @Transactional
    public Bill createBill(CreateBillDto dto, Long userId) {
        OrderType orderType = dto.getOrderType() != null ? dto.getOrderType() : OrderType.DINE_IN;

        CafeTable table = null;
        if (orderType == OrderType.DINE_IN) {
            if (dto.getCafeTableId() == null) {
                throw new IllegalArgumentException("A table must be selected for dine-in orders");
            }
            table = cafeTableService.getById(dto.getCafeTableId());
            if (!table.isActive()) {
                throw new TableNotActiveException("Table '" + table.getName() + "' is not active");
            }
        }

        Customer customer = null;
        if (dto.getCustomerPhone() != null && !dto.getCustomerPhone().isBlank()) {
            customer = customerService.findOrCreateByPhone(
                    dto.getCustomerPhone(), dto.getCustomerName(), userId);
        }

        LocalDate billDate = dto.getBillDate() != null ? dto.getBillDate() : LocalDate.now();
        boolean backdated = !billDate.isEqual(LocalDate.now());

        String billNumber = billNumberService.generateBillNumber(billDate);
        Bill.BillBuilder builder = Bill.builder()
                .billNumber(billNumber)
                .orderType(orderType)
                .cafeTable(table)
                .customer(customer);

        if (orderType == OrderType.ONLINE_ORDER && dto.getOnlineOrderPlatform() != null) {
            builder.onlineOrderPlatform(dto.getOnlineOrderPlatform());
        }

        Bill bill = builder.build();
        bill = billRepository.save(bill);

        if (backdated) {
            billRepository.backdateCreatedAt(bill.getId(), billDate.atTime(LocalTime.now()));
        }

        String staffName = userService.getUserById(userId).getUsername();
        String location = table != null ? table.getName() : orderType.getDisplayName();
        String logMessage = "Bill created for " + location + " by " + staffName
                + (backdated ? " (backdated to " + billDate + ")" : "");
        activityLogService.log(bill.getId(), ActorType.STAFF, staffName, logMessage);

        if (table != null) {
            cafeTableService.markOccupied(table.getId());
        }
        return bill;
    }

    @Transactional
    public Bill addItem(Long billId, AddBillItemDto dto, Long userId) {
        Bill bill = getBillById(billId);
        if (!bill.canAddItems()) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not in DRAFT status");
        }

        MenuItem menuItem = menuService.getItemById(dto.getMenuItemId());
        if (!menuItem.isAvailable()) {
            throw new ResourceNotFoundException("Menu item '" + menuItem.getName() + "' is not available");
        }

        Optional<BillItem> existing = bill.getItems().stream()
                .filter(bi -> bi.getMenuItem().getId().equals(dto.getMenuItemId()))
                .findFirst();

        if (existing.isPresent()) {
            BillItem item = existing.get();
            item.setQuantity(item.getQuantity() + dto.getQuantity());
            item.recalculate();
        } else {
            BillItem newItem = BillItem.builder()
                    .bill(bill)
                    .menuItem(menuItem)
                    .itemName(menuItem.getName())
                    .unitPrice(menuItem.getPrice())
                    .quantity(dto.getQuantity())
                    .lineTotal(menuItem.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity())))
                    .build();
            bill.getItems().add(newItem);
        }

        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String staffName = userService.getUserById(userId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] " + menuItem.getName() + " x" + dto.getQuantity() + " added by " + staffName);

        return saved;
    }

    @Transactional
    public Bill removeItem(Long billId, Long billItemId, Long userId) {
        Bill bill = getBillById(billId);
        if (!bill.isEditable()) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not editable");
        }
        String itemName = bill.getItems().stream()
                .filter(bi -> bi.getId().equals(billItemId))
                .map(BillItem::getItemName)
                .findFirst().orElse("item");
        int qty = bill.getItems().stream()
                .filter(bi -> bi.getId().equals(billItemId))
                .mapToInt(BillItem::getQuantity)
                .findFirst().orElse(0);
        bill.getItems().removeIf(bi -> bi.getId().equals(billItemId));
        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String staffName = userService.getUserById(userId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] " + itemName + " x" + qty + " removed by " + staffName);

        return saved;
    }

    @Transactional
    public Bill updateItemQuantity(Long billId, Long itemId, int quantity, Long userId) {
        if (quantity <= 0) {
            return removeItem(billId, itemId, userId);
        }
        Bill bill = getBillById(billId);
        if (!bill.isEditable()) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not editable");
        }
        final String[] itemName = {""};
        bill.getItems().stream()
                .filter(bi -> bi.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> {
                    itemName[0] = item.getItemName();
                    item.setQuantity(quantity);
                    item.recalculate();
                });
        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String staffName = userService.getUserById(userId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] " + itemName[0] + " qty updated to " + quantity + " by " + staffName);

        return saved;
    }

    @Transactional
    public Bill applyDiscount(Long billId, ApplyDiscountDto dto, Long userId) {
        Bill bill = getBillById(billId);
        if (!bill.isEditable()) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not editable");
        }
        if (dto.getDiscountType() == DiscountType.PERCENTAGE
                && dto.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage discount cannot exceed 100%");
        }
        bill.setDiscountType(dto.getDiscountType());
        bill.setDiscountValue(dto.getDiscountValue());
        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String staffName = userService.getUserById(userId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] Discount applied: " + dto.getDiscountType().getDisplayName()
                        + " " + dto.getDiscountValue() + " by " + staffName);

        return saved;
    }

    @Transactional
    public Bill removeDiscount(Long billId, Long userId) {
        Bill bill = getBillById(billId);
        if (!bill.isEditable()) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not editable");
        }
        bill.setDiscountType(null);
        bill.setDiscountValue(null);
        recalculateTotals(bill);
        return billRepository.save(bill);
    }

    @Transactional
    public Bill confirmBill(Long billId, Long userId) {
        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BillNotEditableException("Only DRAFT bills can be confirmed");
        }
        if (bill.getItems().isEmpty()) {
            throw new EmptyBillException();
        }

        Map<Long, BigDecimal> required = aggregateIngredients(bill);
        validateStock(required);

        for (Map.Entry<Long, BigDecimal> entry : required.entrySet()) {
            inventoryService.deductStock(entry.getKey(), entry.getValue(), bill.getId(), userId);
        }

        bill.setStatus(BillStatus.CONFIRMED);
        bill.setConfirmedAt(java.time.LocalDateTime.now());
        menuService.recomputeAllAutoMode();
        Bill saved = billRepository.save(bill);

        String staffName = userService.getUserById(userId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] Order confirmed by " + staffName);

        return saved;
    }

    @Transactional
    public Bill payBill(Long billId, PayBillDto dto, Long userId) {
        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillStatus.CONFIRMED) {
            throw new BillNotEditableException("Only CONFIRMED bills can be paid");
        }

        Payment payment = Payment.builder()
                .bill(bill)
                .amount(bill.getTotalAmount())
                .method(dto.getMethod())
                .referenceNote(dto.getReferenceNote())
                .createdBy(userId)
                .build();
        paymentRepository.save(payment);

        bill.setStatus(BillStatus.PAID);
        billRepository.save(bill);

        if (bill.getCustomer() != null) {
            Long customerId = bill.getCustomer().getId();
            customerService.recordVisit(customerId, bill.getTotalAmount());
            loyaltyService.earnPoints(customerId, bill.getTotalAmount(), bill.getId(), userId);
        }

        if (bill.isStudentDiscountApplied() && bill.getCustomer() != null) {
            Long customerId = bill.getCustomer().getId();
            customerService.incrementStudentDiscountCount(customerId);
            Customer freshCustomer = customerService.getById(customerId);
            String t2 = LocalTime.now().format(TIME_FMT);
            activityLogService.log(billId, ActorType.SYSTEM, "System",
                    "[" + t2 + "] Student discount availed — total uses: "
                    + freshCustomer.getStudentDiscountCount());
        }

        String staffName = userService.getUserById(userId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] Bill paid via " + dto.getMethod().getDisplayName() + " by " + staffName);

        if (bill.getCafeTable() != null) {
            checkAndFreeTable(bill.getCafeTable().getId());
        }

        if (bill.getCustomer() != null && bill.getCustomer().getPhone() != null) {
            notificationService.send(WhatsappTemplate.BILL_RECEIPT, buildNotificationData(bill));
        }

        return bill;
    }

    @Transactional
    public Bill reopenBill(Long billId, Long userId) {
        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillStatus.CONFIRMED) {
            throw new BillNotEditableException("Only CONFIRMED bills can be reopened for editing");
        }
        reverseStock(bill, userId);
        bill.setStatus(BillStatus.DRAFT);
        return billRepository.save(bill);
    }

    @Transactional
    public Bill linkCustomer(Long billId, String phone, String name, Long userId) {
        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BillNotEditableException("Customer can only be changed on a DRAFT bill");
        }
        Customer customer = customerService.findOrCreateByPhone(phone.trim(), name.trim(), userId);
        bill.setCustomer(customer);
        return billRepository.save(bill);
    }

    @Transactional
    public Bill unlinkCustomer(Long billId, Long userId) {
        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BillNotEditableException("Customer can only be changed on a DRAFT bill");
        }
        if (bill.isStudentDiscountApplied()) {
            bill.setStudentDiscountApplied(false);
            bill.setStudentDiscountAppliedBy(null);
            bill.setStudentDiscountAppliedAt(null);
            bill.setStudentDiscountAmount(BigDecimal.ZERO);
            recalculateTotals(bill);
        }
        bill.setCustomer(null);
        return billRepository.save(bill);
    }

    @Transactional
    public Bill linkCustomerRetroactively(Long billId, String phone, String name, Long userId) {
        Bill bill = getBillById(billId);
        Customer customer = customerService.findOrCreateByPhone(phone.trim(), name.trim(), userId);
        bill.setCustomer(customer);
        String staffName = userService.getUserById(userId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] Customer linked retroactively by " + staffName);
        return billRepository.save(bill);
    }

    @Transactional
    public Bill unlinkCustomerRetroactively(Long billId, Long userId) {
        Bill bill = getBillById(billId);
        bill.setCustomer(null);
        String staffName = userService.getUserById(userId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] Customer unlinked retroactively by " + staffName);
        return billRepository.save(bill);
    }

    @Transactional
    public Bill cancelBill(Long billId, Long userId) {
        Bill bill = getBillById(billId);
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BillNotEditableException("Paid bills cannot be cancelled");
        }
        if (bill.getStatus() == BillStatus.CONFIRMED) {
            reverseStock(bill, userId);
        }
        bill.setStatus(BillStatus.CANCELLED);
        billRepository.save(bill);

        String staffName = userService.getUserById(userId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] Bill cancelled by " + staffName);

        menuService.recomputeAllAutoMode();
        if (bill.getCafeTable() != null) {
            checkAndFreeTable(bill.getCafeTable().getId());
        }
        return bill;
    }

    @Transactional
    public void deleteBill(Long billId, Long userId) {
        Bill bill = getBillById(billId);
        if (!TERMINAL.contains(bill.getStatus())) {
            throw new BillNotEditableException(
                    "Only paid or cancelled bills can be deleted — cancel this bill first");
        }

        String staffName = userService.getUserById(userId).getUsername();
        log.info("Bill {} (id={}) deleted by {}", bill.getBillNumber(), billId, staffName);

        loyaltyService.clearBillReference(billId);
        paymentRepository.findByBillId(billId).ifPresent(paymentRepository::delete);
        billRepository.delete(bill);
    }

    @Transactional
    public Bill applyStudentDiscount(Long billId, Long appliedByUserId) {
        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not editable");
        }
        if (bill.getCustomer() == null) {
            throw new StudentDiscountException(
                    "Bill must have a customer linked to apply student discount");
        }
        if (!bill.getCustomer().isEligibleForStudentDiscount()) {
            throw new StudentDiscountException(
                    "Customer is not marked as a student or has no name on file");
        }
        if (bill.isStudentDiscountApplied()) {
            throw new StudentDiscountException("Student discount already applied to this bill");
        }
        BigDecimal minBill = appConfigService.getDecimal(
                "student.discount.min_bill_amount", new BigDecimal("200.00"));
        if (bill.getSubtotal().compareTo(minBill) < 0) {
            throw new StudentDiscountException(
                    "Bill subtotal ₹" + bill.getSubtotal().setScale(2, RoundingMode.HALF_UP)
                    + " does not meet minimum ₹" + minBill.setScale(2, RoundingMode.HALF_UP)
                    + " for student discount");
        }
        bill.setStudentDiscountApplied(true);
        bill.setStudentDiscountAppliedBy(appliedByUserId);
        bill.setStudentDiscountAppliedAt(LocalDateTime.now());
        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String staffName = userService.getUserById(appliedByUserId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] Student discount applied by " + staffName);
        return saved;
    }

    @Transactional
    public Bill removeStudentDiscount(Long billId, Long removedByUserId) {
        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not editable");
        }
        if (!bill.isStudentDiscountApplied()) {
            return bill;
        }
        bill.setStudentDiscountApplied(false);
        bill.setStudentDiscountAppliedBy(null);
        bill.setStudentDiscountAppliedAt(null);
        bill.setStudentDiscountAmount(BigDecimal.ZERO);
        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String staffName = userService.getUserById(removedByUserId).getUsername();
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.STAFF, staffName,
                "[" + t + "] Student discount removed by " + staffName);
        return saved;
    }

    public String generateWhatsappText(Long billId) {
        Bill bill = getBillById(billId);

        String customerName = bill.getCustomer() != null ? bill.getCustomer().getName() : "there";
        String date = bill.getCreatedAt() != null
                ? bill.getCreatedAt().format(DISPLAY_FMT)
                : "";

        String itemsSummary = bill.getItems().stream()
                .map(i -> i.getItemName() + " x" + i.getQuantity()
                        + " ₹" + i.getLineTotal().setScale(2, RoundingMode.HALF_UP))
                .collect(Collectors.joining(" | "));

        String paidVia = paymentRepository.findByBillId(billId)
                .map(p -> p.getMethod().getDisplayName())
                .orElse("N/A");

        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(customerName)
          .append(". Here is the summary of your bill from Bullet Beats Cafe.\n\n");

        sb.append("*Invoice Details*\n");
        sb.append("• Bill Number: #").append(bill.getBillNumber()).append("\n");
        sb.append("• Date of Issue: ").append(date).append("\n\n");

        sb.append("*Order Summary*\n");
        sb.append("Your ordered items include: ").append(itemsSummary).append("\n\n");

        sb.append("*Payment Details*\n");
        sb.append("• Total Amount Due: ₹")
          .append(bill.getTotalAmount().setScale(2, RoundingMode.HALF_UP)).append("\n");
        sb.append("• Payment Method: ").append(paidVia).append("\n\n");

        sb.append("Have a wonderful day!");
        return sb.toString();
    }

    public void sendBillViaWhatsapp(Long billId) {
        Bill bill = getBillById(billId);
        if (bill.getCustomer() == null || bill.getCustomer().getPhone() == null) {
            throw new IllegalStateException("Bill has no customer or phone number linked");
        }
        notificationService.sendNow(WhatsappTemplate.BILL_RECEIPT, buildNotificationData(bill));
    }

    private BillNotificationData buildNotificationData(Bill bill) {
        NotificationChannel channel = bill.getCustomer().getNotificationPreference() != null
                ? bill.getCustomer().getNotificationPreference()
                : NotificationChannel.WHATSAPP;

        String date = bill.getCreatedAt() != null
                ? bill.getCreatedAt().format(DISPLAY_FMT)
                : "";

        String itemsSummary = bill.getItems().stream()
                .map(i -> i.getItemName() + " x" + i.getQuantity()
                        + " ₹" + i.getLineTotal().setScale(2, RoundingMode.HALF_UP))
                .collect(Collectors.joining(" | "));

        String total = "₹" + bill.getTotalAmount().setScale(2, RoundingMode.HALF_UP);

        String paidVia = paymentRepository.findByBillId(bill.getId())
                .map(p -> p.getMethod().getDisplayName())
                .orElse("N/A");

        return new BillNotificationData(
                bill.getCustomer().getPhone(),
                channel,
                bill.getCustomer().getName(),
                bill.getBillNumber(),
                date,
                itemsSummary,
                total,
                paidVia,
                generateWhatsappText(bill.getId())
        );
    }

    private void recalculateTotals(Bill bill) {
        BigDecimal subtotal = bill.getItems().stream()
                .map(BillItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Student discount (on subtotal, applied first)
        BigDecimal studentDiscountAmount = BigDecimal.ZERO;
        if (bill.isStudentDiscountApplied()
                && bill.getCustomer() != null
                && bill.getCustomer().isEligibleForStudentDiscount()) {
            BigDecimal rate = appConfigService.getDecimal(
                    "student.discount.percentage", new BigDecimal("10.00"));
            studentDiscountAmount = subtotal.multiply(rate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Manager discount (on subtotal, independent)
        BigDecimal managerDiscountAmount = BigDecimal.ZERO;
        if (bill.getDiscountType() != null && bill.getDiscountValue() != null) {
            managerDiscountAmount = switch (bill.getDiscountType()) {
                case FIXED -> bill.getDiscountValue().min(subtotal);
                case PERCENTAGE -> subtotal.multiply(bill.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            };
        }

        // Combined — capped at subtotal
        BigDecimal totalDiscountAmount =
                studentDiscountAmount.add(managerDiscountAmount).min(subtotal);

        BigDecimal gstRate = appConfigService.getDecimal("gst.rate", new BigDecimal("18.00"));
        boolean gstInclusive = appConfigService.getBoolean("gst.inclusive", false);
        BigDecimal deliveryFee = bill.getOrderType() == OrderType.DIRECT_DELIVERY
                ? appConfigService.getDecimal("delivery.fee", BigDecimal.ZERO)
                : BigDecimal.ZERO;

        BigDecimal taxableAmount;
        BigDecimal gstAmount;
        BigDecimal totalAmount;

        if (gstInclusive) {
            BigDecimal grossAfterDiscount = subtotal.subtract(totalDiscountAmount);
            gstAmount = grossAfterDiscount.multiply(gstRate)
                    .divide(BigDecimal.valueOf(100).add(gstRate), 2, RoundingMode.HALF_UP);
            taxableAmount = grossAfterDiscount.subtract(gstAmount);
            totalAmount   = grossAfterDiscount.add(deliveryFee);
        } else {
            taxableAmount = subtotal.subtract(totalDiscountAmount);
            gstAmount     = taxableAmount.multiply(gstRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            totalAmount   = taxableAmount.add(gstAmount).add(deliveryFee);
        }

        bill.setSubtotal(subtotal);
        bill.setStudentDiscountAmount(studentDiscountAmount);
        bill.setDiscountAmount(totalDiscountAmount);
        bill.setTaxableAmount(taxableAmount);
        bill.setGstRate(gstRate);
        bill.setGstInclusive(gstInclusive);
        bill.setGstAmount(gstAmount);
        bill.setDeliveryFee(deliveryFee);
        bill.setTotalAmount(totalAmount);
    }

    private Map<Long, BigDecimal> aggregateIngredients(Bill bill) {
        Map<Long, BigDecimal> required = new HashMap<>();
        for (BillItem billItem : bill.getItems()) {
            MenuItem menuItem = billItem.getMenuItem();
            if (menuItem.getDish() != null) {
                for (DishIngredient ing : menuItem.getDish().getIngredients()) {
                    BigDecimal qty = ing.getQuantityRequired()
                            .multiply(BigDecimal.valueOf(billItem.getQuantity()));
                    required.merge(ing.getGroceryItem().getId(), qty, BigDecimal::add);
                }
            } else if (menuItem.getCombo() != null) {
                for (ComboIngredient ing : menuItem.getCombo().getIngredients()) {
                    BigDecimal qty = ing.getQuantityRequired()
                            .multiply(BigDecimal.valueOf(billItem.getQuantity()));
                    required.merge(ing.getGroceryItem().getId(), qty, BigDecimal::add);
                }
            }
        }
        return required;
    }

    private void validateStock(Map<Long, BigDecimal> required) {
        if (required.isEmpty()) return;
        Map<Long, GroceryItem> byId = groceryItemRepository.findAllById(required.keySet())
                .stream().collect(Collectors.toMap(GroceryItem::getId, g -> g));
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : required.entrySet()) {
            GroceryItem item = byId.get(entry.getKey());
            if (item.getQuantityInStock().compareTo(entry.getValue()) < 0) {
                shortages.add(item.getName() + ": need " + entry.getValue()
                        + " " + item.getUnit() + ", have " + item.getQuantityInStock());
            }
        }
        if (!shortages.isEmpty()) {
            throw new InsufficientStockException(shortages);
        }
    }

    private void reverseStock(Bill bill, Long userId) {
        Map<Long, BigDecimal> required = aggregateIngredients(bill);
        for (Map.Entry<Long, BigDecimal> entry : required.entrySet()) {
            GroceryItem item = inventoryService.getItemById(entry.getKey());
            inventoryService.recordMovement(
                    item,
                    MovementType.INBOUND,
                    entry.getValue(),
                    "BILL_REVERSAL",
                    bill.getId(),
                    "Reversal for cancelled bill " + bill.getBillNumber(),
                    userId);
        }
    }

    private void checkAndFreeTable(Long tableId) {
        long activeBillCount = billRepository.countByCafeTableIdAndStatusNotIn(tableId, TERMINAL);
        if (activeBillCount == 0) {
            cafeTableService.markFree(tableId);
        }
    }

}
