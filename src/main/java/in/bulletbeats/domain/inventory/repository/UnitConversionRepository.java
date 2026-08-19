package in.bulletbeats.domain.inventory.repository;

import in.bulletbeats.domain.inventory.entity.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {

    @Query("SELECT uc FROM UnitConversion uc JOIN FETCH uc.fromUnit JOIN FETCH uc.toUnit " +
           "ORDER BY uc.fromUnit.name, uc.toUnit.name")
    List<UnitConversion> findAllWithUnits();

    boolean existsByFromUnitIdAndToUnitId(Long fromUnitId, Long toUnitId);

    boolean existsByFromUnitIdOrToUnitId(Long fromUnitId, Long toUnitId);

    @Query("SELECT uc FROM UnitConversion uc JOIN FETCH uc.fromUnit JOIN FETCH uc.toUnit " +
           "WHERE lower(uc.fromUnit.name) = lower(:from) AND lower(uc.toUnit.name) = lower(:to)")
    Optional<UnitConversion> findByUnitNames(@Param("from") String from, @Param("to") String to);
}
