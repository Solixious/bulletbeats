package in.bulletbeats.domain.offers.repository;

import in.bulletbeats.domain.offers.entity.OfferRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRedemptionRepository extends JpaRepository<OfferRedemption, Long> {

    long countByOfferId(Long offerId);

    long countByOfferIdAndCustomerId(Long offerId, Long customerId);
}
