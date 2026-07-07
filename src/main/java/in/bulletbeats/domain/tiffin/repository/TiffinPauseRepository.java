package in.bulletbeats.domain.tiffin.repository;

import in.bulletbeats.domain.tiffin.entity.TiffinPause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TiffinPauseRepository extends JpaRepository<TiffinPause, Long> {

    List<TiffinPause> findBySubscriptionIdOrderByPauseFromDesc(Long subscriptionId);

    @Query("SELECT COUNT(p) > 0 FROM TiffinPause p " +
           "WHERE p.subscription.id = :subscriptionId " +
           "AND p.pauseFrom <= :date " +
           "AND (p.pauseUntil IS NULL OR p.pauseUntil >= :date)")
    boolean hasActivePauseOn(Long subscriptionId, LocalDate date);
}
