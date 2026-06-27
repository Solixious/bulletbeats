package in.bulletbeats.web;

import in.bulletbeats.domain.billing.service.BillingService;
import in.bulletbeats.domain.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final InventoryService inventoryService;
    private final BillingService billingService;

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
}
