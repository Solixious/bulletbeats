package in.bulletbeats.domain.offers.repository;

import in.bulletbeats.domain.offers.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findAllByOrderByNameAsc();

    List<Offer> findByActiveTrueAndRequiresCodeFalse();

    List<Offer> findByLegacyStudentDiscountTrue();

    boolean existsByCohortId(Long cohortId);

    @Query("SELECT o FROM Offer o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Offer> findByIdWithItems(Long id);
}
