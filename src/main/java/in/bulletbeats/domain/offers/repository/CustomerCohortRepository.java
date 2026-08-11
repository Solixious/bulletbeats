package in.bulletbeats.domain.offers.repository;

import in.bulletbeats.domain.offers.entity.CustomerCohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerCohortRepository extends JpaRepository<CustomerCohort, Long> {

    @Query("SELECT DISTINCT c FROM CustomerCohort c LEFT JOIN FETCH c.rules ORDER BY c.name ASC")
    List<CustomerCohort> findAllByOrderByNameAsc();

    List<CustomerCohort> findByActiveTrueOrderByNameAsc();

    @Query("SELECT c FROM CustomerCohort c LEFT JOIN FETCH c.rules WHERE c.id = :id")
    Optional<CustomerCohort> findByIdWithRules(Long id);
}
