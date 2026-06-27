package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByIsActiveTrueOrderByNameAsc();

    List<Supplier> findByIsActiveFalseOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
