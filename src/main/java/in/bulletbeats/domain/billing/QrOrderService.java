package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.dto.CategoryWithItemsDto;
import in.bulletbeats.domain.billing.dto.QrMenuDto;
import in.bulletbeats.domain.billing.dto.QrSessionResult;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.BillItem;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.billing.service.BillNumberService;
import in.bulletbeats.domain.billing.service.CafeTableService;
import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.service.CustomerService;
import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.entity.Combo;
import in.bulletbeats.domain.menu.entity.ComboIngredient;
import in.bulletbeats.domain.menu.entity.Dish;
import in.bulletbeats.domain.menu.entity.DishIngredient;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.repository.ComboRepository;
import in.bulletbeats.domain.menu.repository.DishRepository;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.shared.enums.ActorType;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.MovementType;
import in.bulletbeats.domain.shared.exception.BillNotEditableException;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import in.bulletbeats.domain.shared.exception.TableNotActiveException;
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
public class QrOrderService {

    private static final Long SYSTEM_USER_ID = 0L;
    private static final List<BillStatus> ACTIVE_STATUSES = List.of(BillStatus.DRAFT, BillStatus.CONFIRMED);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final BillRepository billRepository;
    private final BillNumberService billNumberService;
    private final CafeTableService cafeTableService;
    private final CustomerService customerService;
    private final MenuService menuService;
    private final CategoryService categoryService;
    private final InventoryService inventoryService;
    private final DishRepository dishRepository;
    private final ComboRepository comboRepository;
    private final ActivityLogService activityLogService;
    private final AppConfigService appConfigService;

    @Transactional
    public QrSessionResult handleQrScan(String qrCode, String phone, String name) {
        CafeTable table = cafeTableService.getByQrCode(qrCode);
        if (!table.isActive()) {
            throw new TableNotActiveException("Table '" + table.getName() + "' is not active");
        }

        table.setLastScannedAt(java.time.LocalDateTime.now());
        // CafeTableService.update persists via its own save; we save directly here
        // table is managed in this transaction, change is flushed automatically

        boolean isReturningCustomer = false;
        Customer customer = null;
        if (phone != null && !phone.isBlank()) {
            isReturningCustomer = customerService.existsByPhone(phone.trim());
            customer = customerService.findOrCreateByPhone(phone.trim(), name, SYSTEM_USER_ID);
        }

        List<Bill> activeBills = billRepository.findByCafeTableIdAndStatusIn(table.getId(), ACTIVE_STATUSES);
        if (!activeBills.isEmpty()) {
            Bill activeBill = activeBills.get(0);
            if (customer != null && activeBill.getCustomer() == null) {
                activeBill.setCustomer(customer);
                billRepository.save(activeBill);
            }
            return new QrSessionResult(table, activeBill, customer, false, isReturningCustomer);
        }

        String billNumber = billNumberService.generateBillNumber();
        Bill bill = Bill.builder()
                .billNumber(billNumber)
                .cafeTable(table)
                .customer(customer)
                .status(BillStatus.DRAFT)
                .build();
        bill = billRepository.save(bill);

        activityLogService.log(bill.getId(), ActorType.SYSTEM, "System",
                "Bill created via QR scan for " + table.getName());

        cafeTableService.markOccupied(table.getId());

        return new QrSessionResult(table, bill, customer, true, isReturningCustomer);
    }

    @Transactional(readOnly = true)
    public QrMenuDto getMenuForQr(Long billId) {
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
        boolean isConfirmed = bill.getStatus() == BillStatus.CONFIRMED;

        return new QrMenuDto(billId, bill.getCafeTable().getName(), customerName, false, isConfirmed, grouped);
    }

    @Transactional
    public Bill addItemViaQr(Long billId, Long menuItemId, int quantity,
                             String customerName, Long customerId) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (!ACTIVE_STATUSES.contains(bill.getStatus())) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not open for ordering");
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
            if (bill.getStatus() == BillStatus.DRAFT) {
                validateStock(aggregateIngredientsForItem(menuItem, newTotalQty));
            } else {
                deductStockForMenuItem(menuItem, quantity, billId);
            }
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

        String actor = customerName != null ? customerName : "QR customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] " + menuItem.getName() + " x" + quantity + " added via QR by " + actor);

        return saved;
    }

    @Transactional
    public Bill removeItemViaQr(Long billId, Long billItemId, String customerName) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (!ACTIVE_STATUSES.contains(bill.getStatus())) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not open for ordering");
        }

        BillItem billItem = bill.getItems().stream()
                .filter(bi -> bi.getId().equals(billItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in bill"));

        String itemName = billItem.getItemName();
        int qty = billItem.getQuantity();

        if (bill.getStatus() == BillStatus.CONFIRMED && !isForceAvailable(billItem.getMenuItem())) {
            reverseStockForBillItem(billItem, billId);
        }

        bill.getItems().remove(billItem);
        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String actor = customerName != null ? customerName : "QR customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] " + itemName + " x" + qty + " removed via QR by " + actor);

        return saved;
    }

    @Transactional
    public Bill confirmViaQr(Long billId, String customerName, Long customerId) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (bill.getStatus() == BillStatus.DRAFT) {
            if (bill.getItems().isEmpty()) {
                throw new BillNotEditableException("Cannot confirm an empty bill");
            }
            Map<Long, BigDecimal> required = aggregateAllIngredients(bill);
            validateStock(required);
            for (Map.Entry<Long, BigDecimal> e : required.entrySet()) {
                inventoryService.deductStock(e.getKey(), e.getValue(), billId, SYSTEM_USER_ID);
            }
            bill.setStatus(BillStatus.CONFIRMED);
            bill.setConfirmedAt(java.time.LocalDateTime.now());
            menuService.recomputeAllAutoMode();
        }

        String actor = customerName != null ? customerName : "QR customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] Order confirmed via QR by " + actor);

        return billRepository.save(bill);
    }

    @Transactional(readOnly = true)
    public Bill getBillForQr(Long billId) {
        return billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Bill> findActiveBillForTable(Long tableId) {
        List<Bill> active = billRepository.findByCafeTableIdAndStatusIn(tableId, ACTIVE_STATUSES);
        return active.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(active.get(0));
    }

    @Transactional
    public Bill updateItemQuantityViaQr(Long billId, Long billItemId, int newQty, String customerName) {
        Bill bill = billRepository.findByIdWithItems(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + billId));

        if (!ACTIVE_STATUSES.contains(bill.getStatus())) {
            throw new BillNotEditableException("Bill " + bill.getBillNumber() + " is not open for ordering");
        }

        if (newQty <= 0) {
            return removeItemViaQr(billId, billItemId, customerName);
        }

        BillItem item = bill.getItems().stream()
                .filter(bi -> bi.getId().equals(billItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in bill"));

        if (!isForceAvailable(item.getMenuItem())) {
            if (bill.getStatus() == BillStatus.DRAFT && newQty > item.getQuantity()) {
                validateStock(aggregateIngredientsForItem(item.getMenuItem(), newQty));
            }
            if (bill.getStatus() == BillStatus.CONFIRMED) {
                int delta = newQty - item.getQuantity();
                if (delta > 0) {
                    deductStockForMenuItem(item.getMenuItem(), delta, billId);
                } else if (delta < 0) {
                    Map<Long, BigDecimal> toReverse = aggregateIngredientsForItem(item.getMenuItem(), -delta);
                    for (Map.Entry<Long, BigDecimal> e : toReverse.entrySet()) {
                        GroceryItem groceryItem = inventoryService.getItemById(e.getKey());
                        inventoryService.recordMovement(groceryItem, MovementType.INBOUND, e.getValue(),
                                "QR_QTY_ADJUST", billId, "QR quantity adjustment", SYSTEM_USER_ID);
                    }
                }
            }
        }

        String itemName = item.getItemName();
        item.setQuantity(newQty);
        item.recalculate();
        recalculateTotals(bill);
        Bill saved = billRepository.save(bill);

        String actor = customerName != null ? customerName : "QR customer";
        String t = LocalTime.now().format(TIME_FMT);
        activityLogService.log(billId, ActorType.CUSTOMER, actor,
                "[" + t + "] " + itemName + " qty updated to " + newQty + " via QR by " + actor);

        return saved;
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void deductStockForMenuItem(MenuItem menuItem, int qty, Long billId) {
        Map<Long, BigDecimal> required = aggregateIngredientsForItem(menuItem, qty);
        validateStock(required);
        for (Map.Entry<Long, BigDecimal> e : required.entrySet()) {
            inventoryService.deductStock(e.getKey(), e.getValue(), billId, SYSTEM_USER_ID);
        }
    }

    private void reverseStockForBillItem(BillItem billItem, Long billId) {
        MenuItem menuItem = billItem.getMenuItem();
        Map<Long, BigDecimal> required = aggregateIngredientsForItem(menuItem, billItem.getQuantity());
        for (Map.Entry<Long, BigDecimal> e : required.entrySet()) {
            GroceryItem groceryItem = inventoryService.getItemById(e.getKey());
            inventoryService.recordMovement(groceryItem, MovementType.INBOUND, e.getValue(),
                    "QR_REMOVE", billId, "QR removal reversal", SYSTEM_USER_ID);
        }
    }

    private Map<Long, BigDecimal> aggregateIngredientsForItem(MenuItem menuItem, int qty) {
        Map<Long, BigDecimal> required = new HashMap<>();
        if (menuItem.getDish() != null) {
            Dish dish = dishRepository.findById(menuItem.getDish().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dish not found"));
            for (DishIngredient ing : dish.getIngredients()) {
                required.merge(ing.getGroceryItem().getId(),
                        ing.getQuantityRequired().multiply(BigDecimal.valueOf(qty)),
                        BigDecimal::add);
            }
        } else if (menuItem.getCombo() != null) {
            Combo combo = comboRepository.findById(menuItem.getCombo().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Combo not found"));
            for (ComboIngredient ing : combo.getIngredients()) {
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
        List<String> shortages = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : required.entrySet()) {
            GroceryItem item = inventoryService.getItemById(e.getKey());
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
