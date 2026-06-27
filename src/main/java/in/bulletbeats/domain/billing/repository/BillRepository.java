package in.bulletbeats.domain.billing.repository;

import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.shared.enums.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {

    @Query("SELECT DISTINCT b FROM Bill b " +
           "LEFT JOIN FETCH b.items bi " +
           "LEFT JOIN FETCH bi.menuItem " +
           "JOIN FETCH b.cafeTable " +
           "LEFT JOIN FETCH b.customer " +
           "WHERE b.id = :id")
    Optional<Bill> findByIdWithItems(@Param("id") Long id);

    List<Bill> findByCafeTableIdAndStatusNotInOrderByCreatedAtDesc(
            Long tableId, Collection<BillStatus> excludedStatuses);

    long countByCafeTableIdAndStatusNotIn(Long tableId, Collection<BillStatus> excludedStatuses);

    List<Bill> findByStatusOrderByCreatedAtDesc(BillStatus status);

    Page<Bill> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    List<Bill> findTop5ByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query(value = "SELECT COUNT(*) FROM bills WHERE DATE(created_at) = CURRENT_DATE", nativeQuery = true)
    long countTodaysBills();

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM bills WHERE status = 'PAID' AND DATE(created_at) = CURRENT_DATE", nativeQuery = true)
    BigDecimal getTodaysRevenue();

    @Query("SELECT b FROM Bill b " +
           "JOIN FETCH b.cafeTable " +
           "LEFT JOIN FETCH b.customer " +
           "WHERE b.status NOT IN :statuses " +
           "ORDER BY b.createdAt DESC")
    List<Bill> findActiveBills(@Param("statuses") Collection<BillStatus> statuses);

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.status NOT IN :statuses")
    long countActiveBills(@Param("statuses") Collection<BillStatus> statuses);

    @Query("SELECT b.cafeTable.id, COUNT(b) FROM Bill b WHERE b.status NOT IN :statuses GROUP BY b.cafeTable.id")
    List<Object[]> countActiveByTableId(@Param("statuses") Collection<BillStatus> statuses);

    List<Bill> findByCafeTableIdAndStatusIn(Long cafeTableId, List<BillStatus> statuses);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM Bill b
            WHERE b.status = 'PAID'
            AND CAST(b.createdAt AS LocalDate) = :date
            """)
    BigDecimal getRevenueForDate(@Param("date") LocalDate date);

    @Query("""
            SELECT COUNT(b)
            FROM Bill b
            WHERE b.status = 'PAID'
            AND CAST(b.createdAt AS LocalDate) = :date
            """)
    long getBillCountForDate(@Param("date") LocalDate date);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM Bill b
            WHERE b.status = 'PAID'
            AND b.createdAt >= :from
            AND b.createdAt < :to
            """)
    BigDecimal getRevenueForRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT COUNT(b)
            FROM Bill b
            WHERE b.status = 'PAID'
            AND b.createdAt >= :from
            AND b.createdAt < :to
            """)
    long getBillCountForRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT COUNT(b)
            FROM Bill b
            WHERE b.status IN ('DRAFT', 'CONFIRMED')
            """)
    long countActiveBills();

}
