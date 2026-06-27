package in.bulletbeats.domain.billing.repository;

import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.shared.enums.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface CafeTableRepository extends JpaRepository<CafeTable, Long> {

    @Query("SELECT t FROM CafeTable t WHERE t.isActive = true ORDER BY t.name ASC")
    List<CafeTable> findByIsActiveTrueOrderByNameAsc();

    Optional<CafeTable> findByQrCode(String qrCode);

    @Query("SELECT t FROM CafeTable t WHERE t.status = :status AND t.isActive = true")
    List<CafeTable> findByStatusAndIsActiveTrue(@Param("status") TableStatus status);

    @Query("SELECT COUNT(t) > 0 FROM CafeTable t WHERE LOWER(t.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT t FROM CafeTable t WHERE t.status = :status AND t.lastScannedAt < :cutoff AND t.isActive = true")
    List<CafeTable> findIdleOccupiedTables(@Param("status") TableStatus status,
                                           @Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT t FROM CafeTable t
            WHERE t.isActive = true
            ORDER BY t.name ASC
            """)
    List<CafeTable> findAllActiveSorted();

    long countByStatusAndIsActiveTrue(TableStatus status);

    @Query("SELECT t FROM CafeTable t WHERE t.floor.id = :floorId AND t.isActive = true ORDER BY t.name ASC")
    List<CafeTable> findActiveByFloorId(@Param("floorId") Long floorId);
}
