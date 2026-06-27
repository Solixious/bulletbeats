package in.bulletbeats.domain.inventory;

import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.inventory.service.PurchaseOrderService;
import in.bulletbeats.domain.inventory.service.ReplenishmentService;
import in.bulletbeats.domain.inventory.service.SupplierService;
import in.bulletbeats.domain.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inventory/replenishment")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class ReplenishmentController {

    private final ReplenishmentService replenishmentService;
    private final PurchaseOrderService purchaseOrderService;
    private final SupplierService supplierService;
    private final InventoryService inventoryService;

    private Long currentUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("requests", replenishmentService.getPendingRequests());
        model.addAttribute("orders", purchaseOrderService.getAllOrders());
        model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
        return "inventory/replenishment/list";
    }

    @PostMapping("/{id}/update-qty")
    public String updateQty(@PathVariable Long id,
                            @RequestParam BigDecimal qty,
                            Model model,
                            HttpServletRequest request) {
        replenishmentService.updateRequestedQty(id, qty);
        if ("true".equals(request.getHeader("HX-Request"))) {
            model.addAttribute("request", replenishmentService.getById(id));
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
            return "inventory/replenishment/fragments/request-row :: request-row";
        }
        return "redirect:/inventory/replenishment";
    }

    @GetMapping("/pending-section")
    public String pendingSection(Model model) {
        model.addAttribute("requests", replenishmentService.getPendingRequests());
        model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
        return "inventory/replenishment/fragments/pending-section :: pending-section";
    }

    @GetMapping("/po-section")
    public String poSection(Model model) {
        model.addAttribute("orders", purchaseOrderService.getAllOrders());
        return "inventory/replenishment/fragments/po-section :: po-section";
    }

    @PostMapping("/{id}/approve")
    public String approveRequest(@PathVariable Long id,
                                 @RequestParam(required = false) Long supplierId,
                                 Authentication auth,
                                 Model model,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        replenishmentService.approveRequest(id, currentUserId(auth), supplierId);
        if ("true".equals(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Trigger", "replenishmentApproved");
            model.addAttribute("request", replenishmentService.getById(id));
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
            return "inventory/replenishment/fragments/request-row :: request-row";
        }
        return "redirect:/inventory/replenishment?approved";
    }

    @PostMapping("/{id}/cancel")
    public String cancelRequest(@PathVariable Long id,
                                Model model,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        replenishmentService.cancelRequest(id);
        if ("true".equals(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Trigger", "replenishmentCancelled");
            model.addAttribute("request", replenishmentService.getById(id));
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
            return "inventory/replenishment/fragments/request-row :: request-row";
        }
        return "redirect:/inventory/replenishment?cancelled";
    }

    @GetMapping("/manual-po-form")
    public String manualPoForm(Model model) {
        model.addAttribute("items", inventoryService.getAllItems());
        model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
        return "inventory/replenishment/fragments/manual-po-form :: manual-po-form";
    }

    @PostMapping("/manual-po")
    public String createManualPO(
            @RequestParam(value = "groceryItemId", required = false) List<Long> groceryItemIds,
            @RequestParam(value = "supplierId", required = false) List<Long> supplierIds,
            @RequestParam(value = "qty", required = false) List<BigDecimal> qtys,
            Model model) {
        purchaseOrderService.createManualPOs(groceryItemIds, supplierIds, qtys);
        model.addAttribute("orders", purchaseOrderService.getAllOrders());
        return "inventory/replenishment/fragments/manual-po-form :: manual-po-created";
    }
}
