package in.bulletbeats.domain.billing;

import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.BillActivityLog;
import in.bulletbeats.domain.billing.repository.BillActivityLogRepository;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.shared.enums.ActorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final BillActivityLogRepository logRepository;
    private final BillRepository billRepository;

    @Transactional
    public void log(Long billId, ActorType actorType, String actorName, String message) {
        Bill billRef = billRepository.getReferenceById(billId);
        BillActivityLog entry = BillActivityLog.builder()
                .bill(billRef)
                .actorType(actorType)
                .actorName(actorName)
                .message(message)
                .build();
        logRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<BillActivityLog> getLogsForBill(Long billId) {
        return logRepository.findByBillIdOrderByCreatedAtDesc(billId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getQrActiveBillIds(List<Long> billIds) {
        if (billIds == null || billIds.isEmpty()) return Set.of();
        return logRepository.findBillIdsWithActorType(billIds, ActorType.CUSTOMER);
    }
}
