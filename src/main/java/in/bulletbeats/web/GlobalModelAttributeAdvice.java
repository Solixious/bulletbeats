package in.bulletbeats.web;

import in.bulletbeats.domain.billing.service.BillingService;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.whatsapp.service.WhatsappMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final InventoryService inventoryService;
    private final BillingService billingService;
    private final WhatsappMessageService whatsappMessageService;

    @ModelAttribute("lowStockCount")
    public long lowStockCount() {
        try {
            return inventoryService.getLowStockCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("activeBillCount")
    public long activeBillCount() {
        try {
            return billingService.countActiveBills();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("whatsappUnreadCount")
    public long whatsappUnreadCount() {
        try {
            return whatsappMessageService.totalUnreadCount();
        } catch (Exception e) {
            return 0;
        }
    }
}
