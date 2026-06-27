package in.bulletbeats.domain.menu.repository;

import in.bulletbeats.domain.menu.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Long> {
    List<Combo> findByIsActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}
