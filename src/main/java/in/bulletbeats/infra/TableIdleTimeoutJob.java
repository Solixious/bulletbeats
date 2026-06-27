package in.bulletbeats.infra;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.ActivityLogService;
import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.billing.repository.CafeTableRepository;
import in.bulletbeats.domain.billing.service.CafeTableService;
import in.bulletbeats.domain.shared.enums.ActorType;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.TableStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TableIdleTimeoutJob {

    private static final List<BillStatus> ACTIVE_STATUSES = List.of(BillStatus.DRAFT, BillStatus.CONFIRMED);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final CafeTableRepository cafeTableRepository;
    private final BillRepository billRepository;
    private final CafeTableService cafeTableService;
    private final ActivityLogService activityLogService;
    private final AppConfigService appConfigService;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
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
            int effectiveTimeout = table.getIdleTimeoutMinutes() > 0
                    ? table.getIdleTimeoutMinutes()
                    : globalTimeout;

            if (table.getLastScannedAt() != null
                    && table.getLastScannedAt().isAfter(LocalDateTime.now().minusMinutes(effectiveTimeout))) {
                continue;
            }

            List<Bill> activeBills = billRepository.findByCafeTableIdAndStatusIn(table.getId(), ACTIVE_STATUSES);

            boolean allEmpty = activeBills.stream().allMatch(b -> b.getItems().isEmpty());

            if (allEmpty) {
                String t = LocalTime.now().format(TIME_FMT);
                for (Bill bill : activeBills) {
                    bill.setStatus(BillStatus.CANCELLED);
                    billRepository.save(bill);
                    activityLogService.log(bill.getId(), ActorType.SYSTEM, "System",
                            "[" + t + "] Bill auto-cancelled — table idle timeout");
                }
                cafeTableService.markFree(table.getId());
                log.info("Table '{}' auto-freed after idle timeout", table.getName());
            } else {
                log.debug("Table '{}' has items — skipping auto-free", table.getName());
            }
        }
    }
}
