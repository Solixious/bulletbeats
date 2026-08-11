package in.bulletbeats.domain.offers.repository;

import in.bulletbeats.domain.offers.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findAllByOrderByNameAsc();

    List<Offer> findByActiveTrueAndRequiresCodeFalse();

    List<Offer> findByLegacyStudentDiscountTrue();

    boolean existsByCohortId(Long cohortId);
}
