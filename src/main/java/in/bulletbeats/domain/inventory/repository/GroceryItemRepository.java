package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, Long> {

    @Query("SELECT g FROM GroceryItem g LEFT JOIN FETCH g.defaultSupplier LEFT JOIN FETCH g.category WHERE g.isActive = true ORDER BY g.name ASC")
    List<GroceryItem> findAllActiveWithSupplier();

    List<GroceryItem> findByIsActiveTrueOrderByNameAsc();

    @Query("SELECT g FROM GroceryItem g LEFT JOIN FETCH g.defaultSupplier LEFT JOIN FETCH g.category WHERE g.id = :id")
    Optional<GroceryItem> findByIdWithSupplier(@Param("id") Long id);

    @Query("SELECT g FROM GroceryItem g WHERE g.isActive = true AND g.quantityInStock < g.minThreshold")
    List<GroceryItem> findLowStockItems();

    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM GroceryItem g " +
           "WHERE lower(g.unit) = lower(:name) OR lower(g.packUnit) = lower(:name) OR lower(g.minorUnit) = lower(:name)")
    boolean existsByAnyUnitField(@Param("name") String name);

    boolean existsByNameIgnoreCase(String name);

    Optional<GroceryItem> findByNameIgnoreCase(String name);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);
}
