package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category WHERE m.isActive = true " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> findByIsActiveTrueOrderByCategoryDisplayOrderAscDisplayOrderAscNameAsc();

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category WHERE m.category.id = :categoryId AND m.isActive = true " +
           "ORDER BY m.displayOrder ASC, m.name ASC")
    List<MenuItem> findByCategoryIdAndIsActiveTrueOrderByDisplayOrderAscNameAsc(@Param("categoryId") Long categoryId);

    List<MenuItem> findByDishId(Long dishId);

    List<MenuItem> findByComboId(Long comboId);

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> findAllWithCategoryOrdered();

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category WHERE m.category.id = :categoryId " +
           "ORDER BY m.displayOrder ASC, m.name ASC")
    List<MenuItem> findAllByCategoryIdWithCategoryOrdered(@Param("categoryId") Long categoryId);

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category WHERE m.id = :id")
    java.util.Optional<MenuItem> findByIdWithCategory(@Param("id") Long id);

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category WHERE m.isActive = true AND m.isAvailable = true " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> findAllAvailableOrdered();

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category " +
           "WHERE m.isActive = true AND m.isAvailable = true " +
           "AND LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> searchAvailableItems(@Param("q") String q);

    @Query("SELECT m FROM MenuItem m JOIN FETCH m.category " +
           "WHERE m.isActive = true " +
           "AND LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY m.category.displayOrder ASC, m.displayOrder ASC, m.name ASC")
    List<MenuItem> searchActiveItems(@Param("q") String q);

    @Query("SELECT m FROM MenuItem m WHERE m.isActive = true AND m.availabilityOverride IS NULL")
    List<MenuItem> findAutoModeItems();
}
