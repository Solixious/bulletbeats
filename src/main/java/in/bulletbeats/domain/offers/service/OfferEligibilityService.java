package in.bulletbeats.domain.offers.service;

import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.offers.entity.Offer;
import in.bulletbeats.domain.offers.entity.OfferCode;
import in.bulletbeats.domain.offers.entity.enums.OfferTargetType;
import in.bulletbeats.domain.offers.repository.OfferCodeRepository;
import in.bulletbeats.domain.offers.repository.OfferRedemptionRepository;
import in.bulletbeats.domain.offers.repository.OfferRepository;
import in.bulletbeats.domain.shared.exception.OfferException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferEligibilityService {

    private final OfferRepository offerRepository;
    private final OfferCodeRepository offerCodeRepository;
    private final OfferRedemptionRepository offerRedemptionRepository;
    private final CohortEvaluationService cohortEvaluationService;

    public record ResolvedOffer(Offer offer, OfferCode code) {}

    public List<Offer> findAutoEligibleOffers(Customer customer, Bill bill) {
        return offerRepository.findByActiveTrueAndRequiresCodeFalse().stream()
                .filter(offer -> isEligible(offer, customer, bill))
                .toList();
    }

    public ResolvedOffer resolveCode(String rawCode, Customer customer, Bill bill) {
        String trimmed = rawCode == null ? "" : rawCode.trim();
        if (trimmed.isEmpty()) {
            throw new OfferException("Enter a code");
        }
        OfferCode code = offerCodeRepository.findByCodeIgnoreCase(trimmed)
                .orElseThrow(() -> new OfferException("Code not found"));
        if (!code.isActive()) {
            throw new OfferException("This code is no longer active");
        }
        if (code.getExpiresAt() != null && LocalDateTime.now().isAfter(code.getExpiresAt())) {
            throw new OfferException("This code has expired");
        }
        if (code.isExhausted()) {
            throw new OfferException("This code has already been fully used");
        }
        if (code.getCustomer() != null) {
            if (customer == null || !code.getCustomer().getId().equals(customer.getId())) {
                throw new OfferException("This code is not valid for this customer");
            }
        }
        Offer offer = code.getOffer();
        if (!offer.isActive()) {
            throw new OfferException("This offer is no longer active");
        }
        if (!offer.isWithinWindow(LocalDateTime.now())) {
            throw new OfferException("This offer is not currently running");
        }
        if (bill.getSubtotal().compareTo(offer.getMinSpend()) < 0) {
            throw new OfferException("Bill does not meet the minimum spend for this offer");
        }
        checkUsageCaps(offer, customer);
        return new ResolvedOffer(offer, code);
    }

    private boolean isEligible(Offer offer, Customer customer, Bill bill) {
        if (offer.isLegacyStudentDiscount()) {
            return customer != null && customer.isEligibleForStudentDiscount();
        }
        if (!offer.isWithinWindow(LocalDateTime.now())) {
            return false;
        }
        if (bill.getSubtotal().compareTo(offer.getMinSpend()) < 0) {
            return false;
        }
        boolean targetMatches = switch (offer.getTargetType()) {
            case ALL -> true;
            case COHORT -> customer != null && cohortEvaluationService.matches(offer.getCohort(), customer);
            case CUSTOMER -> customer != null && offer.getCustomer() != null
                    && offer.getCustomer().getId().equals(customer.getId());
        };
        if (!targetMatches) {
            return false;
        }
        try {
            checkUsageCaps(offer, customer);
        } catch (OfferException e) {
            return false;
        }
        return true;
    }

    private void checkUsageCaps(Offer offer, Customer customer) {
        if (offer.getMaxTotalUses() != null
                && offerRedemptionRepository.countByOfferId(offer.getId()) >= offer.getMaxTotalUses()) {
            throw new OfferException("This offer has reached its usage limit");
        }
        if (offer.getMaxUsesPerCustomer() != null && customer != null
                && offerRedemptionRepository.countByOfferIdAndCustomerId(offer.getId(), customer.getId())
                        >= offer.getMaxUsesPerCustomer()) {
            throw new OfferException("You have already used this offer the maximum number of times");
        }
    }
}
