package in.bulletbeats.domain.offers.repository;

import in.bulletbeats.domain.offers.entity.OfferCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfferCodeRepository extends JpaRepository<OfferCode, Long> {

    Optional<OfferCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<OfferCode> findByOfferIdOrderByCreatedAtDesc(Long offerId);
}
