package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByIsActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Combo c JOIN c.ingredients i " +
           "WHERE i.groceryItem.id = :groceryItemId")
    boolean existsByIngredientsGroceryItemId(@Param("groceryItemId") Long groceryItemId);
}
