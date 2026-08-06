package in.bulletbeats.domain.feedback.repository;

import in.bulletbeats.domain.feedback.entity.FeedbackRequest;
import in.bulletbeats.domain.feedback.entity.FeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeedbackRequestRepository extends JpaRepository<FeedbackRequest, Long> {

    Optional<FeedbackRequest> findFirstByPhoneAndStatusAndExpiresAtAfterOrderByRequestedAtDesc(
            String phone, FeedbackStatus status, LocalDateTime now);

    List<FeedbackRequest> findAllByOrderByRequestedAtDesc();
}
