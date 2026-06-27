package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.PurchaseOrder;
import in.bulletbeats.domain.shared.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("SELECT DISTINCT p FROM PurchaseOrder p JOIN FETCH p.supplier ORDER BY p.createdAt DESC")
    List<PurchaseOrder> findAllWithSupplierOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT p FROM PurchaseOrder p JOIN FETCH p.supplier LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.groceryItem WHERE p.id = :id")
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") Long id);

    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    List<PurchaseOrder> findByStatusOrderByCreatedAtDesc(PurchaseOrderStatus status);

    Optional<PurchaseOrder> findBySupplierIdAndStatus(Long supplierId, PurchaseOrderStatus status);

    @Query("SELECT DISTINCT poi.groceryItem.id FROM PurchaseOrderItem poi " +
           "WHERE poi.purchaseOrder.status IN :statuses")
    Set<Long> findGroceryItemIdsInActiveOrders(@Param("statuses") List<PurchaseOrderStatus> statuses);

    @Query("SELECT CASE WHEN COUNT(poi) > 0 THEN true ELSE false END FROM PurchaseOrderItem poi " +
           "WHERE poi.groceryItem.id = :groceryItemId AND poi.purchaseOrder.status IN :statuses")
    boolean existsActiveOrderForGroceryItem(@Param("groceryItemId") Long groceryItemId,
                                             @Param("statuses") List<PurchaseOrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM PurchaseOrder p " +
           "WHERE p.orderedAt >= :start AND p.orderedAt < :end AND p.totalAmount IS NOT NULL")
    BigDecimal sumSpendForRange(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}
