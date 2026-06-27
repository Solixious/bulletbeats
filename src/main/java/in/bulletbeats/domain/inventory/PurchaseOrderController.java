package in.bulletbeats.domain.inventory;

import in.bulletbeats.domain.inventory.dto.ReceiveItemDto;
import in.bulletbeats.domain.inventory.entity.PurchaseOrder;
import in.bulletbeats.domain.inventory.entity.PurchaseOrderItem;
import in.bulletbeats.domain.inventory.service.PurchaseOrderService;
import in.bulletbeats.domain.shared.enums.PurchaseOrderStatus;
import in.bulletbeats.domain.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/inventory/purchase-orders")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    private Long currentUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @GetMapping
    public String list(Model model) {
        List<PurchaseOrder> orders = purchaseOrderService.getAllOrders();
        Map<PurchaseOrderStatus, List<PurchaseOrder>> grouped = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getStatus));
        model.addAttribute("grouped", grouped);
        model.addAttribute("statuses", PurchaseOrderStatus.values());
        return "inventory/purchase-orders/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("po", purchaseOrderService.getById(id));
        return "inventory/purchase-orders/detail";
    }

    @GetMapping(value = "/{id}/text", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String getWhatsappText(@PathVariable Long id) {
        PurchaseOrder po = purchaseOrderService.getById(id);
        return po.getWhatsappText() != null ? po.getWhatsappText() : "";
    }

    @PostMapping("/{id}/mark-ordered")
    public String markAsOrdered(@PathVariable Long id,
                                @RequestParam(required = false) BigDecimal totalAmount) {
        purchaseOrderService.markAsOrdered(id, totalAmount);
        return "redirect:/inventory/purchase-orders/" + id + "?updated";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Model model, HttpServletRequest request) {
        purchaseOrderService.cancelOrder(id);
        if ("true".equals(request.getHeader("HX-Request"))) {
            model.addAttribute("po", purchaseOrderService.getById(id));
            return "inventory/purchase-orders/fragments/po-detail-sections :: po-cancel-oob";
        }
        return "redirect:/inventory/purchase-orders/" + id;
    }

    @PostMapping("/{poId}/items/{itemId}/update-qty")
    public String updateItemQty(@PathVariable Long poId, @PathVariable Long itemId,
                                @RequestParam BigDecimal qty, Model model,
                                HttpServletRequest request) {
        PurchaseOrder po = purchaseOrderService.updateItemQty(poId, itemId, qty);
        if ("true".equals(request.getHeader("HX-Request"))) {
            PurchaseOrderItem item = po.getItems().stream()
                    .filter(i -> i.getId().equals(itemId))
                    .findFirst().orElseThrow();
            model.addAttribute("po", po);
            model.addAttribute("item", item);
            return "inventory/purchase-orders/fragments/po-detail-sections :: po-item-row";
        }
        return "redirect:/inventory/purchase-orders/" + poId;
    }

    @PostMapping("/{poId}/items/{itemId}/remove")
    public String removeItem(@PathVariable Long poId, @PathVariable Long itemId,
                             Model model, HttpServletRequest request) {
        PurchaseOrder po = purchaseOrderService.removeItem(poId, itemId);
        if ("true".equals(request.getHeader("HX-Request"))) {
            model.addAttribute("po", po);
            if (po.getStatus() == PurchaseOrderStatus.CANCELLED) {
                return "inventory/purchase-orders/fragments/po-detail-sections :: po-cancel-oob";
            }
            return "inventory/purchase-orders/fragments/po-detail-sections :: po-items-section";
        }
        return "redirect:/inventory/purchase-orders/" + poId;
    }

    @PostMapping("/{id}/receive")
    public String receivePO(@PathVariable Long id,
                            @RequestParam("itemId") List<Long> itemIds,
                            @RequestParam("received") List<BigDecimal> receivedQtys,
                            Authentication auth) {
        List<ReceiveItemDto> items = new ArrayList<>();
        for (int i = 0; i < itemIds.size(); i++) {
            ReceiveItemDto dto = new ReceiveItemDto();
            dto.setPurchaseOrderItemId(itemIds.get(i));
            dto.setQuantityReceived(receivedQtys.get(i));
            items.add(dto);
        }
        purchaseOrderService.receivePO(id, items, currentUserId(auth));
        return "redirect:/inventory/purchase-orders/" + id + "?received";
    }
}
