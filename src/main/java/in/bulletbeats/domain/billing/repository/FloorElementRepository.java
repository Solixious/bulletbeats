package in.bulletbeats.domain.billing.repository;

import in.bulletbeats.domain.billing.entity.FloorElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FloorElementRepository extends JpaRepository<FloorElement, Long> {
    List<FloorElement> findByFloorId(Long floorId);
    long countByFloorIdAndElementType(Long floorId, String elementType);
}
