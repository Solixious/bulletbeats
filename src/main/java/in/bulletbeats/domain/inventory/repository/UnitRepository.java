package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<Unit> findByNameIgnoreCase(String name);
}
