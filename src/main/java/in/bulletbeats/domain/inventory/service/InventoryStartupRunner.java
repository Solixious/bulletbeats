package in.bulletbeats.domain.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryStartupRunner implements ApplicationRunner {

    private final InventoryService inventoryService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            inventoryService.backfillReplenishmentRequests();
            log.debug("Inventory replenishment backfill complete");
        } catch (Exception e) {
            log.warn("Replenishment backfill skipped: {}", e.getMessage());
        }
    }
}
