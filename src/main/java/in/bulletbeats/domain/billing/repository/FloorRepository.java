package in.bulletbeats.domain.billing.repository;

import in.bulletbeats.domain.billing.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, Long> {
    List<Floor> findAllByOrderByDisplayOrderAscNameAsc();
}
