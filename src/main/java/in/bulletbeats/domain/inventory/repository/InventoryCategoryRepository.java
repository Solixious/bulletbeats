package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.InventoryCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryCategoryRepository extends JpaRepository<InventoryCategory, Long> {
    List<InventoryCategory> findAllByOrderByDisplayOrderAscNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
