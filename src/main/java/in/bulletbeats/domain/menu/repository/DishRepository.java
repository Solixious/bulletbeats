package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByIsActiveTrueOrderByNameAsc();
    List<Dish> findByIsActiveFalseOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Dish d JOIN d.ingredients i " +
           "WHERE i.groceryItem.id = :groceryItemId")
    boolean existsByIngredientsGroceryItemId(@Param("groceryItemId") Long groceryItemId);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Dish d JOIN d.ingredients i " +
           "WHERE i.preparedItem.id = :preparedItemId")
    boolean existsByIngredientsPreparedItemId(@Param("preparedItemId") Long preparedItemId);
}
