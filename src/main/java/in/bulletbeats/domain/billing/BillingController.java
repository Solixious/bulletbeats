package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.dto.AddBillItemDto;
import in.bulletbeats.domain.billing.dto.ApplyDiscountDto;
import in.bulletbeats.domain.billing.dto.CreateBillDto;
import in.bulletbeats.domain.billing.dto.PayBillDto;
import in.bulletbeats.domain.billing.dto.TableTransferDto;
import in.bulletbeats.domain.billing.dto.TableTransferResult;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.service.BillingService;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.billing.service.CafeTableService;
import in.bulletbeats.domain.crm.service.CustomerService;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.shared.exception.StudentDiscountException;
import in.bulletbeats.domain.user.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final CafeTableService cafeTableService;
    private final MenuService menuService;
    private final CategoryService categoryService;
    private final CustomerService customerService;
    private final ActivityLogService activityLogService;
    private final TableTransferService tableTransferService;
    private final AppConfigService appConfigService;

    // ── List ─────────────────────────────────────────────────────────────

    @GetMapping
    public String list(Model model) {
        List<Bill> activeBills = billingService.getActiveBills();
        List<Long> billIds = activeBills.stream().map(Bill::getId).collect(Collectors.toList());
        model.addAttribute("activeBills", activeBills);
        model.addAttribute("tables", cafeTableService.getAllActive());
        model.addAttribute("activeBillsByTable", billingService.getActiveBillCountByTable());
        model.addAttribute("qrActiveBillIds", activityLogService.getQrActiveBillIds(billIds));
        return "billing/list";
    }

    // ── History ───────────────────────────────────────────────────────────

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String history(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                          @RequestParam(required = false) String phone,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        model.addAttribute("bills", billingService.getHistory(
                from, to, phone, PageRequest.of(page, 25, Sort.by("createdAt").descending())));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("phone", phone != null ? phone.trim() : "");
        return "billing/history";
    }

    // ── New bill ──────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newBillForm(Model model) {
        model.addAttribute("tables", cafeTableService.getAllActive());
        model.addAttribute("activeBillsByTable", billingService.getActiveBillCountByTable());
        model.addAttribute("dto", new CreateBillDto());
        return "billing/new";
    }

    @PostMapping
    public String createBill(@Valid @ModelAttribute("dto") CreateBillDto dto,
                             BindingResult result, Authentication auth, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tables", cafeTableService.getAllActive());
            model.addAttribute("activeBillsByTable", billingService.getActiveBillCountByTable());
            return "billing/new";
        }
        Bill bill = billingService.createBill(dto, currentUserId(auth));
        return "redirect:/bills/" + bill.getId();
    }

    // ── Detail ────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        Bill bill = billingService.getBillById(id);
        model.addAttribute("bill", bill);
        model.addAttribute("billId", id);
        model.addAttribute("isManager", isManager(auth));
        model.addAttribute("categories", categoryService.getAllActive());
        model.addAttribute("menuItems", menuService.getAllItems());
        model.addAttribute("logs", activityLogService.getLogsForBill(id));
        model.addAttribute("studentDiscountPercentage",
                appConfigService.get("student.discount.percentage", "10"));
        model.addAttribute("minBillAmount",
                appConfigService.getDecimal("student.discount.min_bill_amount", new BigDecimal("200.00")));
        return "billing/detail";
    }

    // ── Left panel (HTMX — detects DRAFT→CONFIRMED transition) ──────────

    @GetMapping("/{id}/left-panel")
    public String leftPanel(@PathVariable Long id, Model model,
                            jakarta.servlet.http.HttpServletResponse response) {
        Bill bill = billingService.getBillById(id);
        model.addAttribute("bill", bill);
        if (bill.getStatus() != BillStatus.CONFIRMED) {
            response.setHeader("HX-Reswap", "none");
        }
        return "billing/fragments/kitchen-left-col :: kitchen-left-col";
    }

    // ── Kitchen view poll (HTMX) ─────────────────────────────────────────

    @GetMapping("/{id}/kitchen")
    public String kitchenView(@PathVariable Long id, Model model) {
        model.addAttribute("bill", billingService.getBillById(id));
        return "billing/fragments/kitchen-view :: kitchen-view";
    }

    // ── Bill panel poll (HTMX) ───────────────────────────────────────────

    @GetMapping("/{id}/panel")
    public String billPanel(@PathVariable Long id, Authentication auth, Model model) {
        return billPanelResponse(billingService.getBillById(id), auth, model);
    }

    // ── Bill status poll (HTMX) ──────────────────────────────────────────

    @GetMapping("/{id}/bill-status")
    public String billStatusFragment(@PathVariable Long id, Authentication auth, Model model) {
        Bill bill = billingService.getBillById(id);
        model.addAttribute("bill", bill);
        model.addAttribute("isManager", isManager(auth));
        return "billing/fragments/bill-status :: bill-status";
    }

    // ── Activity log (HTMX) ───────────────────────────────────────────────

    @GetMapping("/{id}/activity-log")
    public String activityLog(@PathVariable Long id, Model model) {
        model.addAttribute("logs", activityLogService.getLogsForBill(id));
        model.addAttribute("billId", id);
        return "billing/fragments/activity-log :: activity-log";
    }

    // ── Transfer form (HTMX, Manager+) ───────────────────────────────────

    @GetMapping("/{id}/transfer-form")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String transferForm(@PathVariable Long id, Model model) {
        Bill bill = billingService.getBillById(id);
        Long currentTableId = bill.getCafeTable().getId();
        List<CafeTable> freeTables = cafeTableService.getAllActive().stream()
                .filter(t -> t.isFree() && !t.getId().equals(currentTableId))
                .collect(Collectors.toList());
        model.addAttribute("bill", bill);
        model.addAttribute("freeTables", freeTables);
        model.addAttribute("dto", new TableTransferDto());
        return "billing/fragments/transfer-form :: transfer-form";
    }

    // ── Execute transfer (HTMX, Manager+) ────────────────────────────────

    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String transferTable(@PathVariable Long id,
                                @Valid @ModelAttribute TableTransferDto dto,
                                Authentication auth, Model model) {
        Bill bill = billingService.getBillById(id);
        TableTransferResult result = tableTransferService.transferTable(
                bill.getCafeTable().getId(), dto.getToTableId(), currentUserId(auth));
        model.addAttribute("result", result);
        model.addAttribute("billId", id);
        return "billing/fragments/transfer-result :: transfer-result";
    }

    // ── Menu browser (HTMX) ───────────────────────────────────────────────

    @GetMapping("/{id}/menu")
    public String menuItems(@PathVariable Long id,
                            @RequestParam(required = false) Long categoryId,
                            Model model) {
        model.addAttribute("menuItems", categoryId != null
                ? menuService.getItemsByCategory(categoryId)
                : menuService.getAllItems());
        model.addAttribute("billId", id);
        return "billing/fragments/menu-grid :: menu-grid";
    }

    @GetMapping("/{id}/menu-search")
    public String menuSearch(@PathVariable Long id,
                             @RequestParam(defaultValue = "") String q,
                             Model model) {
        model.addAttribute("menuItems", menuService.searchActiveItems(q.trim()));
        model.addAttribute("billId", id);
        return "billing/fragments/menu-grid :: menu-grid";
    }

    // ── Customer search (HTMX) ────────────────────────────────────────────

    @GetMapping("/customer-search")
    public String customerSearch(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("q", q.trim());
        model.addAttribute("customers", q.isBlank() ? java.util.List.of()
                : customerService.search(q.trim()));
        return "billing/fragments/customer-results :: customer-results";
    }

    // ── Add / remove / update items (HTMX) ───────────────────────────────

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable Long id,
                          @ModelAttribute AddBillItemDto dto,
                          Authentication auth, Model model) {
        Bill bill = billingService.addItem(id, dto, currentUserId(auth));
        return billPanelResponse(bill, auth, model);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public String removeItem(@PathVariable Long id, @PathVariable Long itemId,
                             Authentication auth, Model model) {
        Bill bill = billingService.removeItem(id, itemId, currentUserId(auth));
        return billPanelResponse(bill, auth, model);
    }

    @PatchMapping("/{id}/items/{itemId}/quantity")
    public String updateQuantity(@PathVariable Long id, @PathVariable Long itemId,
                                 @RequestParam int quantity,
                                 Authentication auth, Model model) {
        Bill bill = billingService.updateItemQuantity(id, itemId, quantity, currentUserId(auth));
        return billPanelResponse(bill, auth, model);
    }

    // ── Customer student status (HTMX, all roles) ────────────────────────

    @PostMapping("/{id}/customer/mark-student")
    public String markCustomerAsStudent(@PathVariable Long id, Authentication auth, Model model) {
        Bill bill = billingService.getBillById(id);
        if (bill.getCustomer() == null) {
            return billPanelResponse(bill, auth, model);
        }
        try {
            customerService.markAsStudent(bill.getCustomer().getId(), currentUserId(auth));
        } catch (IllegalStateException ignored) {
            // customer has no name — button should be disabled in UI
            return billPanelResponse(billingService.getBillById(id), auth, model);
        }
        // Auto-apply discount immediately if the bill already meets the threshold
        try {
            bill = billingService.applyStudentDiscount(id, currentUserId(auth));
        } catch (StudentDiscountException ignored) {
            // Bill is below threshold — panel will show the Apply button for later
            bill = billingService.getBillById(id);
        }
        return billPanelResponse(bill, auth, model);
    }

    @PostMapping("/{id}/customer/unmark-student")
    public String unmarkCustomerAsStudent(@PathVariable Long id, Authentication auth, Model model) {
        Bill bill = billingService.getBillById(id);
        if (bill.getCustomer() != null) {
            customerService.unmarkAsStudent(bill.getCustomer().getId(), currentUserId(auth));
            if (bill.isStudentDiscountApplied()) {
                bill = billingService.removeStudentDiscount(id, currentUserId(auth));
                return billPanelResponse(bill, auth, model);
            }
        }
        return billPanelResponse(billingService.getBillById(id), auth, model);
    }

    // ── Student discount (HTMX) ──────────────────────────────────────────

    @PostMapping("/{id}/student-discount")
    public String applyStudentDiscount(@PathVariable Long id, Authentication auth,
                                        Model model, HttpServletResponse response) {
        try {
            Bill bill = billingService.applyStudentDiscount(id, currentUserId(auth));
            return billPanelResponse(bill, auth, model);
        } catch (StudentDiscountException e) {
            response.setStatus(HttpServletResponse.SC_UNPROCESSABLE_CONTENT);
            response.setHeader("HX-Retarget", "#student-discount-error");
            response.setHeader("HX-Reswap", "outerHTML");
            model.addAttribute("reason", e.getReason());
            return "billing/fragments/student-discount-error :: student-discount-error";
        }
    }

    @DeleteMapping("/{id}/student-discount")
    public String removeStudentDiscount(@PathVariable Long id, Authentication auth, Model model) {
        Bill bill = billingService.removeStudentDiscount(id, currentUserId(auth));
        return billPanelResponse(bill, auth, model);
    }

    // ── Discount (HTMX, Manager+) ─────────────────────────────────────────

    @PostMapping("/{id}/discount")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String applyDiscount(@PathVariable Long id,
                                @Valid @ModelAttribute ApplyDiscountDto dto,
                                Authentication auth, Model model) {
        Bill bill = billingService.applyDiscount(id, dto, currentUserId(auth));
        return billPanelResponse(bill, auth, model);
    }

    @DeleteMapping("/{id}/discount")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String removeDiscount(@PathVariable Long id,
                                 Authentication auth, Model model) {
        Bill bill = billingService.removeDiscount(id, currentUserId(auth));
        return billPanelResponse(bill, auth, model);
    }

    // ── Confirm (HTMX) ────────────────────────────────────────────────────

    @PostMapping("/{id}/confirm")
    public String confirmBill(@PathVariable Long id, Authentication auth, Model model) {
        try {
            billingService.confirmBill(id, currentUserId(auth));
            Bill bill = billingService.getBillById(id);
            model.addAttribute("bill", bill);
            model.addAttribute("isManager", isManager(auth));
            return "billing/fragments/confirm-success :: confirm-success";
        } catch (InsufficientStockException e) {
            model.addAttribute("stockDetails", e.getDetails());
            model.addAttribute("billId", id);
            return "billing/fragments/stock-error :: stock-error";
        }
    }

    // ── Pay (HTMX) ────────────────────────────────────────────────────────

    @PostMapping("/{id}/pay")
    public String payBill(@PathVariable Long id,
                          @Valid @ModelAttribute PayBillDto dto,
                          Authentication auth, Model model) {
        billingService.payBill(id, dto, currentUserId(auth));
        Bill bill = billingService.getBillById(id);
        model.addAttribute("bill", bill);
        model.addAttribute("isManager", isManager(auth));
        return "billing/fragments/pay-success :: pay-success";
    }

    // ── Reopen (Manager+) ─────────────────────────────────────────────────

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String reopenBill(@PathVariable Long id, Authentication auth) {
        billingService.reopenBill(id, currentUserId(auth));
        return "redirect:/bills/" + id;
    }

    // ── Cancel (Manager+) ─────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String cancelBill(@PathVariable Long id, Authentication auth) {
        billingService.cancelBill(id, currentUserId(auth));
        return "redirect:/bills?cancelled";
    }

    // ── WhatsApp (HTMX) ───────────────────────────────────────────────────

    @PostMapping("/{id}/whatsapp-send")
    public String whatsappSend(@PathVariable Long id, Model model) {
        billingService.sendBillViaWhatsapp(id);
        return "billing/fragments/whatsapp-stub :: whatsapp-stub";
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String billPanelResponse(Bill bill, Authentication auth, Model model) {
        model.addAttribute("bill", bill);
        model.addAttribute("billId", bill.getId());
        model.addAttribute("isManager", isManager(auth));
        model.addAttribute("studentDiscountPercentage",
                appConfigService.get("student.discount.percentage", "10"));
        model.addAttribute("minBillAmount",
                appConfigService.getDecimal("student.discount.min_bill_amount", new BigDecimal("200.00")));
        return "billing/fragments/bill-panel :: bill-panel";
    }

    private Long currentUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    private boolean isManager(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")
                            || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
