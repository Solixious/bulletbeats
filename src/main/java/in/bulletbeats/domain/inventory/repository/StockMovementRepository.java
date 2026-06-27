package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByGroceryItemIdOrderByCreatedAtDesc(Long groceryItemId);

    Page<StockMovement> findByGroceryItemId(Long id, Pageable pageable);
}
