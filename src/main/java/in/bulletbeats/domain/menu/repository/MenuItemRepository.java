package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category c LEFT JOIN FETCH c.parent " +
           "LEFT JOIN FETCH m.dish LEFT JOIN FETCH m.combo " +
           "WHERE m.isActive = true " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> findByIsActiveTrueOrderByCategoryDisplayOrderAscDisplayOrderAscNameAsc();

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category c LEFT JOIN FETCH c.parent " +
           "LEFT JOIN FETCH m.dish LEFT JOIN FETCH m.combo " +
           "WHERE m.category.id = :categoryId AND m.isActive = true " +
           "ORDER BY m.displayOrder ASC, m.name ASC")
    List<MenuItem> findByCategoryIdAndIsActiveTrueOrderByDisplayOrderAscNameAsc(@Param("categoryId") Long categoryId);

    List<MenuItem> findByDishId(Long dishId);

    List<MenuItem> findByComboId(Long comboId);

    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category c LEFT JOIN FETCH c.parent " +
           "LEFT JOIN FETCH m.dish LEFT JOIN FETCH m.combo " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> findAllWithCategoryOrdered();

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category c LEFT JOIN FETCH c.parent " +
           "LEFT JOIN FETCH m.dish LEFT JOIN FETCH m.combo " +
           "WHERE m.category.id = :categoryId " +
           "ORDER BY m.displayOrder ASC, m.name ASC")
    List<MenuItem> findAllByCategoryIdWithCategoryOrdered(@Param("categoryId") Long categoryId);

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category LEFT JOIN FETCH m.dish LEFT JOIN FETCH m.combo " +
           "WHERE m.id = :id")
    java.util.Optional<MenuItem> findByIdWithCategory(@Param("id") Long id);

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category LEFT JOIN FETCH m.dish LEFT JOIN FETCH m.combo " +
           "WHERE m.isActive = true AND m.isAvailable = true " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> findAllAvailableOrdered();

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category c LEFT JOIN FETCH c.parent " +
           "LEFT JOIN FETCH m.dish LEFT JOIN FETCH m.combo " +
           "WHERE m.isActive = true AND m.isAvailable = true " +
           "AND LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> searchAvailableItems(@Param("q") String q);

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category c LEFT JOIN FETCH c.parent " +
           "LEFT JOIN FETCH m.dish LEFT JOIN FETCH m.combo " +
           "WHERE m.isActive = true " +
           "AND LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> searchActiveItems(@Param("q") String q);

    @Query("SELECT m FROM MenuItem m WHERE m.isActive = true AND m.availabilityOverride IS NULL")
    List<MenuItem> findAutoModeItems();

    @Query("SELECT DISTINCT m FROM MenuItem m " +
           "LEFT JOIN FETCH m.dish " +
           "LEFT JOIN FETCH m.combo " +
           "WHERE m.isActive = true AND m.availabilityOverride IS NULL")
    List<MenuItem> findAutoModeItemsWithRecipes();

    @Query("SELECT m FROM MenuItem m WHERE m.promoted = true")
    List<MenuItem> findAllPromoted();

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category WHERE m.isActive = true AND m.promoted = true")
    java.util.Optional<MenuItem> findActivePromoted();

    long countByIsActiveTrue();

    /**
     * Sales summary for every active menu item, scoped to [from, to) when both are given, or
     * all-time when both are null — powers the Reports page (top sellers / slow movers / never
     * sold), including items with zero sales in the scope so dead stock and slow movers surface.
     */
    @Query(value = """
            SELECT
              mi.id AS itemId,
              mi.name AS itemName,
              c.name AS categoryName,
              mi.price AS price,
              COALESCE(SUM(CASE WHEN b.status = 'PAID'
                                 AND (CAST(:from AS timestamp) IS NULL OR b.created_at >= :from)
                                 AND (CAST(:to AS timestamp) IS NULL OR b.created_at < :to)
                            THEN bi.quantity END), 0) AS quantity,
              COALESCE(SUM(CASE WHEN b.status = 'PAID'
                                 AND (CAST(:from AS timestamp) IS NULL OR b.created_at >= :from)
                                 AND (CAST(:to AS timestamp) IS NULL OR b.created_at < :to)
                            THEN bi.line_total END), 0) AS revenue,
              MAX(CASE WHEN b.status = 'PAID'
                            AND (CAST(:from AS timestamp) IS NULL OR b.created_at >= :from)
                            AND (CAST(:to AS timestamp) IS NULL OR b.created_at < :to)
                       THEN b.created_at END) AS lastSoldAt
            FROM menu_items mi
            JOIN categories c ON c.id = mi.category_id
            LEFT JOIN bill_items bi ON bi.menu_item_id = mi.id
            LEFT JOIN bills b ON b.id = bi.bill_id
            WHERE mi.is_active = true
            GROUP BY mi.id, mi.name, c.name, mi.price
            ORDER BY quantity ASC, mi.name ASC
            """, nativeQuery = true)
    List<Object[]> findActiveItemsSalesSummary(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
