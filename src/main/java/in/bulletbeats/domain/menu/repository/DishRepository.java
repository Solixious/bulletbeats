package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByIsActiveTrueOrderByNameAsc();
    List<Dish> findByIsActiveFalseOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
