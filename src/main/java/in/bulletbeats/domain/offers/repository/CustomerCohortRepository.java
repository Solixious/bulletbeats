package in.bulletbeats.domain.offers.repository;

import in.bulletbeats.domain.offers.entity.CustomerCohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerCohortRepository extends JpaRepository<CustomerCohort, Long> {

    List<CustomerCohort> findAllByOrderByNameAsc();

    List<CustomerCohort> findByActiveTrueOrderByNameAsc();
}
