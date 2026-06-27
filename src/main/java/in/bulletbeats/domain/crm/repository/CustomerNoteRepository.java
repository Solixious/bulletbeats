package in.bulletbeats.domain.crm.repository;

import in.bulletbeats.domain.crm.entity.CustomerNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

    List<CustomerNote> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
