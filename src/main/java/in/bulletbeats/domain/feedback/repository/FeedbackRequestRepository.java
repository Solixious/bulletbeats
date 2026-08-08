package in.bulletbeats.domain.feedback.repository;

import in.bulletbeats.domain.feedback.entity.FeedbackRequest;
import in.bulletbeats.domain.feedback.entity.FeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedbackRequestRepository extends JpaRepository<FeedbackRequest, Long> {

    Optional<FeedbackRequest> findFirstByPhoneAndStatusAndExpiresAtAfterOrderByRequestedAtDesc(
            String phone, FeedbackStatus status, LocalDateTime now);

    @Query("SELECT r FROM FeedbackRequest r " +
           "LEFT JOIN FETCH r.bill " +
           "LEFT JOIN FETCH r.customer " +
           "ORDER BY r.requestedAt DESC")
    List<FeedbackRequest> findAllByOrderByRequestedAtDesc();

    List<FeedbackRequest> findByBillIdOrderByRequestedAtDesc(Long billId);
}
