package in.bulletbeats.domain.tiffin.repository;

import in.bulletbeats.domain.tiffin.TiffinStatus;
import in.bulletbeats.domain.tiffin.entity.TiffinSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TiffinSubscriptionRepository extends JpaRepository<TiffinSubscription, Long> {

    @Query("SELECT s FROM TiffinSubscription s JOIN FETCH s.customer c " +
           "WHERE s.status IN :statuses ORDER BY c.name ASC")
    List<TiffinSubscription> findByStatusInWithCustomer(List<TiffinStatus> statuses);

    @Query("SELECT s FROM TiffinSubscription s JOIN FETCH s.customer c ORDER BY c.name ASC")
    List<TiffinSubscription> findAllWithCustomer();

    boolean existsByCustomerIdAndStatusIn(Long customerId, List<TiffinStatus> statuses);
}
