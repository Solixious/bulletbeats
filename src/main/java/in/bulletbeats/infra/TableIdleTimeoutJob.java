package in.bulletbeats.infra;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.repository.CafeTableRepository;
import in.bulletbeats.domain.billing.service.CafeTableService;
import in.bulletbeats.domain.shared.enums.TableStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TableIdleTimeoutJob {

    private final CafeTableRepository cafeTableRepository;
    private final CafeTableService cafeTableService;
    private final AppConfigService appConfigService;

    @Scheduled(fixedDelay = 60_000)
    public void checkIdleTables() {
        int globalTimeout;
        try {
            globalTimeout = Integer.parseInt(
                    appConfigService.get("table.idle.timeout.minutes", "10"));
        } catch (NumberFormatException e) {
            globalTimeout = 10;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(globalTimeout);
        List<CafeTable> idleTables = cafeTableRepository.findIdleOccupiedTables(TableStatus.OCCUPIED, cutoff);

        for (CafeTable table : idleTables) {
            try {
                // cancelIfStillIdle re-reads the table and its bills fresh in its own
                // transaction and re-checks every condition — this loop only supplies
                // candidates, it never acts on the (possibly stale) snapshot above.
                cafeTableService.cancelIfStillIdle(table.getId(), globalTimeout);
            } catch (ObjectOptimisticLockingFailureException e) {
                // A bill on this table was edited concurrently (e.g. an item was added)
                // right as we tried to cancel it. Skip it — it's no longer idle, and if
                // it genuinely is later, the next sweep (60s) will catch it.
                log.debug("Table '{}' skipped — bill was modified concurrently", table.getName());
            }
        }
    }
}
