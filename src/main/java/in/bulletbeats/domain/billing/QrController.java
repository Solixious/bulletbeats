package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.dto.QrAddItemDto;
import in.bulletbeats.domain.billing.dto.QrConfirmDto;
import in.bulletbeats.domain.billing.dto.QrSessionResult;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.BillActivityLog;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.service.CafeTableService;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/qr")
@RequiredArgsConstructor
public class QrController {

    private final QrOrderService qrOrderService;
    private final CafeTableService cafeTableService;
    private final MenuService menuService;
    private final ActivityLogService activityLogService;
    private final AppConfigService appConfigService;

    // ── Landing ───────────────────────────────────────────────────────────

    @GetMapping("/{qrCode}")
    public String landing(@PathVariable String qrCode,
                          RedirectAttributes ra,
                          Model model) {
        CafeTable table = cafeTableService.getByQrCode(qrCode);
        Optional<Bill> activeBill = qrOrderService.findActiveBillForTable(table.getId());

        if (activeBill.isPresent()) {
            ra.addAttribute("billId", activeBill.get().getId());
            ra.addAttribute("returning", true);
            return "redirect:/qr/" + qrCode + "/menu";
        }

        model.addAttribute("table", table);
        model.addAttribute("qrCode", qrCode);
        model.addAttribute("cafeName", appConfigService.get("cafe.name", "Bullet Beats Café"));
        return "qr/landing";
    }

    // ── Start session ─────────────────────────────────────────────────────

    @PostMapping("/{qrCode}/start")
    public String start(@PathVariable String qrCode,
                        @RequestParam(required = false) String phone,
                        @RequestParam(required = false) String name,
                        RedirectAttributes ra) {
        QrSessionResult result = qrOrderService.handleQrScan(qrCode, phone, name);

        ra.addAttribute("billId", result.bill().getId());
        ra.addAttribute("returning", result.isReturningCustomer());
        if (result.customer() != null) {
            ra.addAttribute("customerId", result.customer().getId());
            ra.addAttribute("customerName",
                    result.customer().getName() != null ? result.customer().getName() : "Guest");
        } else {
            ra.addAttribute("customerName", "Guest");
        }
        return "redirect:/qr/" + qrCode + "/menu";
    }

    // ── Main menu page ────────────────────────────────────────────────────

    @GetMapping("/{qrCode}/menu")
    public String menu(@PathVariable String qrCode,
                       @RequestParam Long billId,
                       @RequestParam(required = false) Long customerId,
                       @RequestParam(required = false) String customerName,
                       @RequestParam(required = false) boolean returning,
                       Model model) {
        Bill bill = qrOrderService.getBillForQr(billId);
        model.addAttribute("bill", bill);
        model.addAttribute("menuDto", qrOrderService.getMenuForQr(billId));
        model.addAttribute("qrCode", qrCode);
        model.addAttribute("billId", billId);
        model.addAttribute("customerId", customerId);
        model.addAttribute("customerName", customerName != null ? customerName : "Guest");
        model.addAttribute("returning", returning);
        model.addAttribute("cafeName", appConfigService.get("cafe.name", "Bullet Beats Café"));
        return "qr/menu";
    }

    // ── HTMX: add item ────────────────────────────────────────────────────

    @PostMapping("/{qrCode}/items")
    public String addItem(@PathVariable String qrCode,
                          @Valid @ModelAttribute QrAddItemDto dto,
                          Model model) {
        try {
            Bill bill = qrOrderService.addItemViaQr(
                    dto.getBillId(), dto.getMenuItemId(), dto.getQuantity(),
                    dto.getCustomerName(), dto.getCustomerId());
            return orderPanelResponse(bill, qrCode, dto.getCustomerName(), model);
        } catch (InsufficientStockException e) {
            model.addAttribute("stockError", "Sorry, this item isn't available right now. Please try something else.");
            Bill bill = qrOrderService.getBillForQr(dto.getBillId());
            return orderPanelResponse(bill, qrCode, dto.getCustomerName(), model);
        }
    }

    // ── HTMX: update quantity ─────────────────────────────────────────────

    @PatchMapping("/{qrCode}/items/{billItemId}/quantity")
    public String updateQuantity(@PathVariable String qrCode,
                                 @PathVariable Long billItemId,
                                 @RequestParam Long billId,
                                 @RequestParam int quantity,
                                 @RequestParam(required = false) String customerName,
                                 Model model) {
        try {
            Bill bill = qrOrderService.updateItemQuantityViaQr(billId, billItemId, quantity, customerName);
            return orderPanelResponse(bill, qrCode, customerName, model);
        } catch (InsufficientStockException e) {
            model.addAttribute("stockError", "Sorry, that quantity isn't available right now.");
            Bill bill = qrOrderService.getBillForQr(billId);
            return orderPanelResponse(bill, qrCode, customerName, model);
        }
    }

    // ── HTMX: remove item ─────────────────────────────────────────────────

    @DeleteMapping("/{qrCode}/items/{billItemId}")
    public String removeItem(@PathVariable String qrCode,
                             @PathVariable Long billItemId,
                             @RequestParam Long billId,
                             @RequestParam(required = false) String customerName,
                             Model model) {
        qrOrderService.removeItemViaQr(billId, billItemId, customerName);
        Bill bill = qrOrderService.getBillForQr(billId);
        return orderPanelResponse(bill, qrCode, customerName, model);
    }

    // ── HTMX: confirm / checkout ──────────────────────────────────────────

    @PostMapping("/{qrCode}/confirm")
    public String confirm(@PathVariable String qrCode,
                          @ModelAttribute QrConfirmDto dto,
                          Model model) {
        try {
            Bill bill = qrOrderService.confirmViaQr(dto.getBillId(), dto.getCustomerName(), dto.getCustomerId());

            List<BillActivityLog> allLogs = activityLogService.getLogsForBill(bill.getId());
            List<BillActivityLog> lastFive = allLogs.size() > 5
                    ? allLogs.subList(allLogs.size() - 5, allLogs.size())
                    : allLogs;

            model.addAttribute("bill", bill);
            model.addAttribute("qrCode", qrCode);
            model.addAttribute("customerName", dto.getCustomerName());
            model.addAttribute("customerId", dto.getCustomerId());
            model.addAttribute("logs", lastFive);
            return "qr/fragments/confirm-result :: confirm-result";
        } catch (InsufficientStockException e) {
            Bill bill = qrOrderService.getBillForQr(dto.getBillId());
            model.addAttribute("stockError", "Sorry, one or more items in your order aren't available right now. Please remove them and try again, or ask a staff member for help.");
            return orderPanelResponse(bill, qrCode, dto.getCustomerName(), model);
        }
    }

    // ── HTMX: poll ───────────────────────────────────────────────────────

    @GetMapping("/{qrCode}/poll")
    public String poll(@PathVariable String qrCode,
                       @RequestParam Long billId,
                       @RequestParam(required = false) String customerName,
                       Model model) {
        Bill bill = qrOrderService.getBillForQr(billId);
        if (bill.getStatus() == BillStatus.PAID) {
            model.addAttribute("bill", bill);
            model.addAttribute("customerName", customerName != null ? customerName : "Guest");
            return "qr/fragments/payment-done :: payment-done";
        }
        return orderPanelResponse(bill, qrCode, customerName, model);
    }

    // ── HTMX: category filter ─────────────────────────────────────────────

    @GetMapping("/{qrCode}/menu-items")
    public String menuItems(@PathVariable String qrCode,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam Long billId,
                            @RequestParam(required = false) String customerName,
                            @RequestParam(required = false) Long customerId,
                            Model model) {
        List<MenuItem> items = categoryId != null
                ? menuService.getItemsByCategory(categoryId)
                : menuService.getAllAvailableItems();
        Bill bill = qrOrderService.getBillForQr(billId);
        model.addAttribute("items", items);
        model.addAttribute("bill", bill);
        model.addAttribute("billId", billId);
        model.addAttribute("qrCode", qrCode);
        model.addAttribute("customerName", customerName);
        model.addAttribute("customerId", customerId);
        return "qr/fragments/menu-grid :: qr-menu-grid";
    }

    // ── Error handling ────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "qr/error";
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String orderPanelResponse(Bill bill, String qrCode, String customerName, Model model) {
        model.addAttribute("bill", bill);
        model.addAttribute("qrCode", qrCode);
        model.addAttribute("customerName", customerName != null ? customerName : "Guest");
        return "qr/fragments/order-panel :: order-panel";
    }
}
