package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.MenuItemAvailabilityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemAvailabilityLogRepository extends JpaRepository<MenuItemAvailabilityLog, Long> {
    List<MenuItemAvailabilityLog> findByMenuItemIdOrderByChangedAtDesc(Long menuItemId);
}
