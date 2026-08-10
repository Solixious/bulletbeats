package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.dto.CategoryWithItemsDto;
import in.bulletbeats.domain.billing.dto.DeliveryHoursStatus;
import in.bulletbeats.domain.billing.dto.DeliveryStartResult;
import in.bulletbeats.domain.billing.dto.QrAddItemDto;
import in.bulletbeats.domain.billing.dto.QrConfirmDto;
import in.bulletbeats.domain.billing.dto.SubcategoryWithItemsDto;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.BillActivityLog;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.exception.BillNotEditableException;
import in.bulletbeats.domain.shared.exception.DeliveryClosedException;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryOrderService deliveryOrderService;
    private final MenuService menuService;
    private final CategoryService categoryService;
    private final ActivityLogService activityLogService;
    private final AppConfigService appConfigService;

    // ── Landing ───────────────────────────────────────────────────────────

    @GetMapping
    public String landing(Model model) {
        model.addAttribute("cafeName", appConfigService.get("cafe.name", "Bullet Beats Café"));
        model.addAttribute("hoursStatus", deliveryOrderService.getHoursStatus());
        return "delivery/landing";
    }

    // ── Start order ──────────────────────────────────────────────────────

    @PostMapping("/start")
    public String start(@RequestParam(required = false) String phone,
                        @RequestParam(required = false) String name,
                        @RequestParam(required = false) String address,
                        RedirectAttributes ra,
                        Model model) {
        try {
            DeliveryStartResult result = deliveryOrderService.startOrder(phone, name, address);
            ra.addAttribute("customerName",
                    result.customer().getName() != null ? result.customer().getName() : "Guest");
            return "redirect:/order/" + result.bill().getId() + "/menu";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cafeName", appConfigService.get("cafe.name", "Bullet Beats Café"));
            model.addAttribute("hoursStatus", deliveryOrderService.getHoursStatus());
            return "delivery/landing";
        }
    }

    // ── Main menu page ────────────────────────────────────────────────────

    @GetMapping("/{billId}/menu")
    public String menu(@PathVariable Long billId,
                       @RequestParam(required = false) String customerName,
                       Model model) {
        Bill bill = deliveryOrderService.getOrder(billId);
        DeliveryHoursStatus hoursStatus = deliveryOrderService.getHoursStatus();
        model.addAttribute("bill", bill);
        model.addAttribute("menuDto", deliveryOrderService.getMenuForOrder(billId));
        model.addAttribute("billId", billId);
        model.addAttribute("customerName", customerName != null ? customerName : "Guest");
        model.addAttribute("locked", bill.getStatus().isTerminal());
        model.addAttribute("cafeName", appConfigService.get("cafe.name", "Bullet Beats Café"));
        model.addAttribute("hoursStatus", hoursStatus);
        model.addAttribute("promotedItem", bill.getStatus() == BillStatus.DRAFT && !hoursStatus.closed()
                ? menuService.getPromotedItem().orElse(null) : null);
        return "delivery/menu";
    }

    // ── HTMX: add item ────────────────────────────────────────────────────

    @PostMapping("/{billId}/items")
    public String addItem(@PathVariable Long billId,
                          @Valid @ModelAttribute QrAddItemDto dto,
                          Model model) {
        try {
            Bill bill = deliveryOrderService.addItem(billId, dto.getMenuItemId(), dto.getQuantity(), dto.getCustomerName());
            return orderPanelResponse(bill, dto.getCustomerName(), model);
        } catch (InsufficientStockException e) {
            model.addAttribute("stockError", "Sorry, this item isn't available right now. Please try something else.");
            Bill bill = deliveryOrderService.getOrder(billId);
            return orderPanelResponse(bill, dto.getCustomerName(), model);
        } catch (DeliveryClosedException e) {
            model.addAttribute("closedError", e.getMessage());
            Bill bill = deliveryOrderService.getOrder(billId);
            return orderPanelResponse(bill, dto.getCustomerName(), model);
        }
    }

    // ── HTMX: update quantity ─────────────────────────────────────────────

    @PatchMapping("/{billId}/items/{billItemId}/quantity")
    public String updateQuantity(@PathVariable Long billId,
                                 @PathVariable Long billItemId,
                                 @RequestParam int quantity,
                                 @RequestParam(required = false) String customerName,
                                 Model model) {
        try {
            Bill bill = deliveryOrderService.updateItemQuantity(billId, billItemId, quantity, customerName);
            return orderPanelResponse(bill, customerName, model);
        } catch (InsufficientStockException e) {
            model.addAttribute("stockError", "Sorry, that quantity isn't available right now.");
            Bill bill = deliveryOrderService.getOrder(billId);
            return orderPanelResponse(bill, customerName, model);
        } catch (DeliveryClosedException e) {
            model.addAttribute("closedError", e.getMessage());
            Bill bill = deliveryOrderService.getOrder(billId);
            return orderPanelResponse(bill, customerName, model);
        }
    }

    // ── HTMX: remove item ─────────────────────────────────────────────────

    @DeleteMapping("/{billId}/items/{billItemId}")
    public String removeItem(@PathVariable Long billId,
                             @PathVariable Long billItemId,
                             @RequestParam(required = false) String customerName,
                             Model model) {
        Bill bill = deliveryOrderService.removeItem(billId, billItemId, customerName);
        return orderPanelResponse(bill, customerName, model);
    }

    // ── HTMX: confirm / place order ────────────────────────────────────────

    @PostMapping("/{billId}/confirm")
    public String confirm(@PathVariable Long billId,
                          @ModelAttribute QrConfirmDto dto,
                          Model model) {
        try {
            Bill bill = deliveryOrderService.confirmOrder(billId, dto.getCustomerName());

            List<BillActivityLog> allLogs = activityLogService.getLogsForBill(bill.getId());
            List<BillActivityLog> lastFive = allLogs.size() > 5
                    ? allLogs.subList(allLogs.size() - 5, allLogs.size())
                    : allLogs;

            model.addAttribute("bill", bill);
            model.addAttribute("customerName", dto.getCustomerName());
            model.addAttribute("logs", lastFive);
            return "delivery/fragments/confirm-result :: confirm-result";
        } catch (InsufficientStockException e) {
            Bill bill = deliveryOrderService.getOrder(billId);
            model.addAttribute("stockError", "Sorry, one or more items in your order aren't available right now. Please remove them and try again.");
            return orderPanelResponse(bill, dto.getCustomerName(), model);
        }
    }

    // ── HTMX: poll ───────────────────────────────────────────────────────

    @GetMapping("/{billId}/poll")
    public String poll(@PathVariable Long billId,
                       @RequestParam(required = false) String customerName,
                       Model model) {
        Bill bill = deliveryOrderService.getOrder(billId);
        if (bill.getStatus().isTerminal()) {
            List<BillActivityLog> allLogs = activityLogService.getLogsForBill(bill.getId());
            List<BillActivityLog> lastFive = allLogs.size() > 5
                    ? allLogs.subList(allLogs.size() - 5, allLogs.size())
                    : allLogs;
            model.addAttribute("bill", bill);
            model.addAttribute("customerName", customerName != null ? customerName : "Guest");
            model.addAttribute("logs", lastFive);
            return "delivery/fragments/confirm-result :: confirm-result";
        }
        return orderPanelResponse(bill, customerName, model);
    }

    // ── HTMX: category filter ─────────────────────────────────────────────

    @GetMapping("/{billId}/menu-items")
    public String menuItems(@PathVariable Long billId,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) String customerName,
                            Model model) {
        List<MenuItem> items = List.of();
        List<SubcategoryWithItemsDto> subcategoryGroups = List.of();
        List<CategoryWithItemsDto> categories = List.of();
        if (categoryId == null) {
            categories = deliveryOrderService.getGroupedMenuItems();
        } else {
            items = menuService.getItemsByCategory(categoryId);
            Category selected = categoryService.getById(categoryId);
            if (selected.getParent() == null) {
                subcategoryGroups = categoryService.getActiveSubcategories(categoryId).stream()
                        .map(sub -> new SubcategoryWithItemsDto(sub, menuService.getItemsByCategory(sub.getId())))
                        .filter(g -> !g.items().isEmpty())
                        .toList();
            }
        }
        Bill bill = deliveryOrderService.getOrder(billId);
        model.addAttribute("items", items);
        model.addAttribute("subcategoryGroups", subcategoryGroups);
        model.addAttribute("categories", categories);
        model.addAttribute("bill", bill);
        model.addAttribute("billId", billId);
        model.addAttribute("customerName", customerName);
        model.addAttribute("locked", bill.getStatus().isTerminal());
        model.addAttribute("hoursStatus", deliveryOrderService.getHoursStatus());
        return "delivery/fragments/menu-grid :: delivery-menu-grid";
    }

    // ── Error handling ────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "delivery/error";
    }

    @ExceptionHandler(BillNotEditableException.class)
    public String handleNotEditable(BillNotEditableException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "delivery/error";
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String orderPanelResponse(Bill bill, String customerName, Model model) {
        model.addAttribute("bill", bill);
        model.addAttribute("customerName", customerName != null ? customerName : "Guest");
        model.addAttribute("hoursStatus", deliveryOrderService.getHoursStatus());
        return "delivery/fragments/order-panel :: order-panel";
    }
}
