package in.bulletbeats.domain.billing.repository;

import in.bulletbeats.domain.billing.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {

    @Query(value = """
            SELECT
              bi.item_name AS itemName,
              SUM(bi.quantity) AS quantityToday,
              SUM(bi.line_total) AS revenueToday
            FROM bill_items bi
            JOIN bills b ON b.id = bi.bill_id
            WHERE b.status = 'PAID'
            AND DATE(b.created_at) = :date
            GROUP BY bi.item_name
            ORDER BY SUM(bi.quantity) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopItemsForDate(@Param("date") LocalDate date, @Param("limit") int limit);
}
