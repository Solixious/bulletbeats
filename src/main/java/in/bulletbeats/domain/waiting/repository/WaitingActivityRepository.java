package in.bulletbeats.domain.waiting.repository;

import in.bulletbeats.domain.waiting.entity.WaitingActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaitingActivityRepository extends JpaRepository<WaitingActivity, Long> {

    List<WaitingActivity> findAllByOrderByCategoryAscSortOrderAscNameAsc();

    List<WaitingActivity> findAllByIsActiveTrueOrderByCategoryAscSortOrderAscNameAsc();

    boolean existsByIsActiveTrue();
}
