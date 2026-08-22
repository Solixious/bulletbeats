package in.bulletbeats.domain.platform.repository;

import in.bulletbeats.domain.platform.entity.OnlinePlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnlinePlatformRepository extends JpaRepository<OnlinePlatform, Long> {

    List<OnlinePlatform> findByIsActiveTrueOrderByNameAsc();

    List<OnlinePlatform> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
