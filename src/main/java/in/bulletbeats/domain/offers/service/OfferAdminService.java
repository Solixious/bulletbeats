package in.bulletbeats.domain.offers.service;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.repository.CustomerRepository;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.repository.CategoryRepository;
import in.bulletbeats.domain.menu.repository.MenuItemRepository;
import in.bulletbeats.domain.offers.dto.OfferCodeDto;
import in.bulletbeats.domain.offers.dto.OfferDto;
import in.bulletbeats.domain.offers.entity.CustomerCohort;
import in.bulletbeats.domain.offers.entity.Offer;
import in.bulletbeats.domain.offers.entity.OfferCode;
import in.bulletbeats.domain.offers.entity.OfferItemScope;
import in.bulletbeats.domain.offers.entity.enums.CodeUsageType;
import in.bulletbeats.domain.offers.entity.enums.OfferTargetType;
import in.bulletbeats.domain.offers.repository.CustomerCohortRepository;
import in.bulletbeats.domain.offers.repository.OfferCodeRepository;
import in.bulletbeats.domain.offers.repository.OfferRedemptionRepository;
import in.bulletbeats.domain.offers.repository.OfferRepository;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferAdminService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OfferRepository offerRepository;
    private final OfferCodeRepository offerCodeRepository;
    private final OfferRedemptionRepository offerRedemptionRepository;
    private final CustomerCohortRepository cohortRepository;
    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    public List<Offer> getAll() {
        return offerRepository.findAllByOrderByNameAsc();
    }

    public Offer getById(Long id) {
        return offerRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
    }

    @Transactional
    public Offer create(OfferDto dto) {
        Offer offer = Offer.builder().build();
        applyDto(offer, dto);
        return offerRepository.save(offer);
    }

    @Transactional
    public Offer update(Long id, OfferDto dto) {
        Offer offer = getById(id);
        if (offer.isSystem()) {
            throw new IllegalArgumentException("System offers cannot be edited");
        }
        applyDto(offer, dto);
        return offerRepository.save(offer);
    }

    private void applyDto(Offer offer, OfferDto dto) {
        validateMechanismFields(dto);

        CustomerCohort cohort = null;
        Customer customer = null;
        if (dto.getTargetType() == OfferTargetType.COHORT) {
            if (dto.getCohortId() == null) {
                throw new IllegalArgumentException("Select a cohort for a cohort-targeted offer");
            }
            cohort = cohortRepository.findById(dto.getCohortId())
                    .orElseThrow(() -> new IllegalArgumentException("Cohort not found"));
        } else if (dto.getTargetType() == OfferTargetType.CUSTOMER) {
            if (dto.getCustomerPhone() == null || dto.getCustomerPhone().isBlank()) {
                throw new IllegalArgumentException("Enter a customer phone number for a single-customer offer");
            }
            customer = customerRepository.findByPhone(dto.getCustomerPhone().trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No customer found with phone " + dto.getCustomerPhone()));
        }

        offer.setName(dto.getName());
        offer.setDescription(dto.getDescription());
        offer.setMechanism(dto.getMechanism());
        offer.setPercentageValue(dto.getPercentageValue());
        offer.setFixedValue(dto.getFixedValue());
        offer.setBuyQuantity(dto.getBuyQuantity());
        offer.setGetQuantity(dto.getGetQuantity());
        offer.setTargetType(dto.getTargetType());
        offer.setCohort(cohort);
        offer.setCustomer(customer);
        offer.setMinSpend(dto.getMinSpend() != null ? dto.getMinSpend() : java.math.BigDecimal.ZERO);
        offer.setStartsAt(dto.getStartsAt());
        offer.setEndsAt(dto.getEndsAt());
        offer.setRequiresCode(dto.isRequiresCode());
        offer.setMaxTotalUses(dto.getMaxTotalUses());
        offer.setMaxUsesPerCustomer(dto.getMaxUsesPerCustomer());
        offer.setActive(dto.isActive());

        offer.getItems().clear();
        if (offer.isItemScoped()) {
            List<Long> menuItemIds = dto.getMenuItemIds() == null ? List.of() : dto.getMenuItemIds();
            List<Long> categoryIds = dto.getCategoryIds() == null ? List.of() : dto.getCategoryIds();
            if (menuItemIds.isEmpty() && categoryIds.isEmpty()) {
                throw new IllegalArgumentException(
                        "Select at least one item or category for an item-scoped offer");
            }
            List<MenuItem> menuItems = menuItemRepository.findAllById(menuItemIds);
            List<Category> categories = categoryRepository.findAllById(categoryIds);
            List<OfferItemScope> scopes = new ArrayList<>();
            for (MenuItem item : menuItems) {
                scopes.add(OfferItemScope.builder().offer(offer).menuItem(item).build());
            }
            for (Category category : categories) {
                scopes.add(OfferItemScope.builder().offer(offer).category(category).build());
            }
            offer.getItems().addAll(scopes);
        }
    }

    private void validateMechanismFields(OfferDto dto) {
        switch (dto.getMechanism()) {
            case BILL_PERCENTAGE, ITEM_PERCENTAGE -> {
                if (dto.getPercentageValue() == null || dto.getPercentageValue().signum() <= 0) {
                    throw new IllegalArgumentException("Enter a percentage value greater than 0");
                }
            }
            case BILL_FIXED, ITEM_FIXED -> {
                if (dto.getFixedValue() == null || dto.getFixedValue().signum() <= 0) {
                    throw new IllegalArgumentException("Enter a fixed amount greater than 0");
                }
            }
            case BUY_X_GET_Y_FREE -> {
                if (dto.getBuyQuantity() == null || dto.getBuyQuantity() <= 0
                        || dto.getGetQuantity() == null || dto.getGetQuantity() <= 0) {
                    throw new IllegalArgumentException("Enter a buy quantity and a get quantity, both greater than 0");
                }
            }
        }
    }

    @Transactional
    public void toggleActive(Long id) {
        Offer offer = getById(id);
        offer.setActive(!offer.isActive());
        offerRepository.save(offer);
    }

    @Transactional
    public void delete(Long id) {
        Offer offer = getById(id);
        if (offer.isSystem()) {
            throw new IllegalArgumentException("System offers cannot be deleted");
        }
        if (offerRedemptionRepository.countByOfferId(id) > 0) {
            throw new IllegalArgumentException("This offer has already been redeemed and cannot be deleted — deactivate it instead");
        }
        offerRepository.delete(offer);
    }

    public List<OfferCode> getCodes(Long offerId) {
        return offerCodeRepository.findByOfferIdOrderByCreatedAtDesc(offerId);
    }

    @Transactional
    public OfferCode generateCode(Long offerId, OfferCodeDto dto) {
        Offer offer = getById(offerId);

        Customer customer = null;
        if (dto.getCustomerPhone() != null && !dto.getCustomerPhone().isBlank()) {
            customer = customerRepository.findByPhone(dto.getCustomerPhone().trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No customer found with phone " + dto.getCustomerPhone()));
        }

        Integer maxUses = switch (dto.getUsageType()) {
            case SINGLE_USE -> 1;
            case MULTI_USE -> null;
            case LIMITED_USE -> {
                if (dto.getMaxUses() == null || dto.getMaxUses() <= 0) {
                    throw new IllegalArgumentException("Enter a positive max-uses count for a limited-use code");
                }
                yield dto.getMaxUses();
            }
        };

        String code = (dto.getCode() != null && !dto.getCode().isBlank())
                ? dto.getCode().trim().toUpperCase()
                : generateRandomCode();
        if (offerCodeRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Code '" + code + "' is already in use");
        }

        OfferCode offerCode = OfferCode.builder()
                .offer(offer)
                .code(code)
                .customer(customer)
                .usageType(dto.getUsageType())
                .maxUses(maxUses)
                .active(true)
                .expiresAt(dto.getExpiresAt() != null ? dto.getExpiresAt() : offer.getEndsAt())
                .build();
        return offerCodeRepository.save(offerCode);
    }

    @Transactional
    public void setCodeActive(Long codeId, boolean active) {
        OfferCode code = offerCodeRepository.findById(codeId)
                .orElseThrow(() -> new ResourceNotFoundException("Code not found with id: " + codeId));
        code.setActive(active);
        offerCodeRepository.save(code);
    }

    @Transactional
    public void deleteCode(Long codeId) {
        offerCodeRepository.deleteById(codeId);
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder("OFR-");
        for (int i = 0; i < 8; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
