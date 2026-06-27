package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByMenuItemIdOrderByChangedAtDesc(Long menuItemId);
}
