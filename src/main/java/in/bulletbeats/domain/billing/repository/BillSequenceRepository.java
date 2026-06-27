package in.bulletbeats.domain.billing.repository;

import in.bulletbeats.domain.billing.entity.BillSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface BillSequenceRepository extends JpaRepository<BillSequence, LocalDate> {
}
