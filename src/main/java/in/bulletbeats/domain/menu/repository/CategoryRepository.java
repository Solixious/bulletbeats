package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsActiveTrueOrderByDisplayOrderAscNameAsc();
    List<Category> findByIsActiveFalseOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
