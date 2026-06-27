package in.bulletbeats.domain.billing.repository;

import in.bulletbeats.domain.billing.entity.BillActivityLog;
import in.bulletbeats.domain.shared.enums.ActorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface BillActivityLogRepository extends JpaRepository<BillActivityLog, Long> {

    List<BillActivityLog> findByBillIdOrderByCreatedAtDesc(Long billId);

    @Query("SELECT DISTINCT l.bill.id FROM BillActivityLog l WHERE l.actorType = :actorType AND l.bill.id IN :billIds")
    Set<Long> findBillIdsWithActorType(@Param("billIds") List<Long> billIds,
                                       @Param("actorType") ActorType actorType);
}
