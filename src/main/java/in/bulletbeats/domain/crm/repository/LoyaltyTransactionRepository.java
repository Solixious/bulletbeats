package in.bulletbeats.domain.crm.repository;

import in.bulletbeats.domain.crm.entity.LoyaltyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {

    List<LoyaltyTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<LoyaltyTransaction> findByCustomerId(Long customerId, Pageable pageable);
}
