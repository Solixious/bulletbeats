package in.bulletbeats.domain.billing.repository;

import in.bulletbeats.domain.billing.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {

    /** Most recent note this customer left per menu item, across all their bills — used to prefill the add-to-cart note field. */
    @Query(value = """
            SELECT DISTINCT ON (bi.menu_item_id)
              bi.menu_item_id AS menuItemId,
              bi.note AS note
            FROM bill_items bi
            JOIN bills b ON b.id = bi.bill_id
            WHERE b.customer_id = :customerId
              AND bi.note IS NOT NULL
            ORDER BY bi.menu_item_id, bi.created_at DESC
            """, nativeQuery = true)
    List<MenuItemNoteRow> findLatestNotesByCustomer(@Param("customerId") Long customerId);

    interface MenuItemNoteRow {
        Long getMenuItemId();
        String getNote();
    }
}
