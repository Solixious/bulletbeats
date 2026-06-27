package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, Long> {

    @Query("SELECT g FROM GroceryItem g LEFT JOIN FETCH g.defaultSupplier WHERE g.isActive = true ORDER BY g.name ASC")
    List<GroceryItem> findAllActiveWithSupplier();

    List<GroceryItem> findByIsActiveTrueOrderByNameAsc();

    @Query("SELECT g FROM GroceryItem g LEFT JOIN FETCH g.defaultSupplier WHERE g.id = :id")
    Optional<GroceryItem> findByIdWithSupplier(@Param("id") Long id);

    @Query("SELECT g FROM GroceryItem g WHERE g.isActive = true AND g.quantityInStock <= g.minThreshold")
    List<GroceryItem> findLowStockItems();

    @Query("SELECT DISTINCT lower(g.unit) FROM GroceryItem g " +
           "WHERE lower(g.unit) LIKE lower(:prefix) || '%' " +
           "ORDER BY lower(g.unit)")
    List<String> findDistinctUnitsByPrefix(@Param("prefix") String prefix);

    boolean existsByNameIgnoreCase(String name);

    Optional<GroceryItem> findByNameIgnoreCase(String name);
}
