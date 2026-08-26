package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.PreparedItemStockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreparedItemStockMovementRepository extends JpaRepository<PreparedItemStockMovement, Long> {
    Page<PreparedItemStockMovement> findByPreparedItemId(Long preparedItemId, Pageable pageable);
    boolean existsByPreparedItemId(Long preparedItemId);
}
