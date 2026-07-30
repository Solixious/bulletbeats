package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAscNameAsc();
    List<Category> findByParentIsNullAndIsActiveFalseOrderByNameAsc();
    List<Category> findByParentIsNullOrderByDisplayOrderAscNameAsc();
    List<Category> findByParentIdAndIsActiveTrueOrderByDisplayOrderAscNameAsc(Long parentId);
    List<Category> findByParentIdOrderByDisplayOrderAscNameAsc(Long parentId);
    boolean existsByParentId(Long parentId);
    boolean existsByNameIgnoreCaseAndParentIsNull(String name);
    boolean existsByNameIgnoreCaseAndParentId(String name, Long parentId);
}
