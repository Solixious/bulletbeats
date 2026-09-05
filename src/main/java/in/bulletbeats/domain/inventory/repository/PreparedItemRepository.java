package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.PreparedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PreparedItemRepository extends JpaRepository<PreparedItem, Long> {

    List<PreparedItem> findByIsActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM PreparedItem p JOIN p.ingredients i " +
           "WHERE i.groceryItem.id = :groceryItemId")
    boolean existsByIngredientsGroceryItemId(@Param("groceryItemId") Long groceryItemId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM PreparedItem p JOIN p.ingredients i " +
           "WHERE i.ingredientPreparedItem.id = :preparedItemId")
    boolean existsByIngredientsIngredientPreparedItemId(@Param("preparedItemId") Long preparedItemId);
}
