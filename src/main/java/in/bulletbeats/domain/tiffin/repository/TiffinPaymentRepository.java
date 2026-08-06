package in.bulletbeats.domain.tiffin.repository;

import in.bulletbeats.domain.tiffin.entity.TiffinPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TiffinPaymentRepository extends JpaRepository<TiffinPayment, Long> {

    List<TiffinPayment> findBySubscriptionIdOrderByCoverageFromDesc(Long subscriptionId);

    @Query("SELECT MAX(p.coverageUntil) FROM TiffinPayment p WHERE p.subscription.id = :subscriptionId")
    Optional<LocalDate> findPaidThroughDate(Long subscriptionId);

    @Query("SELECT p.subscription.id, MAX(p.coverageUntil) FROM TiffinPayment p " +
           "WHERE p.subscription.id IN :subscriptionIds GROUP BY p.subscription.id")
    List<Object[]> findPaidThroughDates(List<Long> subscriptionIds);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM TiffinPayment p WHERE p.subscription.id = :subscriptionId")
    BigDecimal sumAmountPaidForSubscription(Long subscriptionId);
}
