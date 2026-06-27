package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.ReplenishmentRequest;
import in.bulletbeats.domain.shared.enums.ReplenishmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReplenishmentRequestRepository extends JpaRepository<ReplenishmentRequest, Long> {

    @Query("SELECT r FROM ReplenishmentRequest r JOIN FETCH r.groceryItem gi LEFT JOIN FETCH gi.defaultSupplier WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<ReplenishmentRequest> findByStatusWithItem(@Param("status") ReplenishmentStatus status);

    @Query("SELECT r FROM ReplenishmentRequest r JOIN FETCH r.groceryItem gi LEFT JOIN FETCH gi.defaultSupplier WHERE r.id = :id")
    Optional<ReplenishmentRequest> findByIdWithItem(@Param("id") Long id);

    List<ReplenishmentRequest> findByStatusOrderByCreatedAtDesc(ReplenishmentStatus status);

    boolean existsByGroceryItemIdAndStatus(Long groceryItemId, ReplenishmentStatus status);

    List<ReplenishmentRequest> findByGroceryItemIdAndStatus(Long groceryItemId, ReplenishmentStatus status);

    List<ReplenishmentRequest> findByGroceryItemIdOrderByCreatedAtDesc(Long groceryItemId);

    long countByStatus(ReplenishmentStatus status);
}
