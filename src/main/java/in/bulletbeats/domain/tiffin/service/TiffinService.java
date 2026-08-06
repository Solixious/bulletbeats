package in.bulletbeats.domain.tiffin.service;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.repository.CustomerRepository;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import in.bulletbeats.domain.tiffin.TiffinMealType;
import in.bulletbeats.domain.tiffin.TiffinStatus;
import in.bulletbeats.domain.tiffin.dto.TiffinPauseDto;
import in.bulletbeats.domain.tiffin.dto.TiffinPaymentDto;
import in.bulletbeats.domain.tiffin.dto.TiffinPricingDto;
import in.bulletbeats.domain.tiffin.dto.TiffinPricingOverrideDto;
import in.bulletbeats.domain.tiffin.dto.TiffinSubscriptionDto;
import in.bulletbeats.domain.tiffin.entity.TiffinPause;
import in.bulletbeats.domain.tiffin.entity.TiffinPayment;
import in.bulletbeats.domain.tiffin.entity.TiffinPricing;
import in.bulletbeats.domain.tiffin.entity.TiffinSubscription;
import in.bulletbeats.domain.tiffin.repository.TiffinPauseRepository;
import in.bulletbeats.domain.tiffin.repository.TiffinPaymentRepository;
import in.bulletbeats.domain.tiffin.repository.TiffinPricingRepository;
import in.bulletbeats.domain.tiffin.repository.TiffinSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TiffinService {

    private final TiffinSubscriptionRepository subscriptionRepository;
    private final TiffinPricingRepository pricingRepository;
    private final TiffinPauseRepository pauseRepository;
    private final TiffinPaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    public List<TiffinSubscription> getActiveAndPaused() {
        return subscriptionRepository.findByStatusInWithCustomer(
                List.of(TiffinStatus.ACTIVE, TiffinStatus.PAUSED));
    }

    public List<TiffinSubscription> getAll() {
        return subscriptionRepository.findAllWithCustomer();
    }

    public TiffinSubscription getById(Long id) {
        return subscriptionRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tiffin subscription not found"));
    }

    public boolean hasActiveSubscription(Long customerId) {
        return subscriptionRepository.existsByCustomerIdAndStatusIn(
                customerId, List.of(TiffinStatus.ACTIVE, TiffinStatus.PAUSED));
    }

    @Transactional
    public TiffinSubscription subscribe(TiffinSubscriptionDto dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        TiffinSubscription sub = TiffinSubscription.builder()
                .customer(customer)
                .status(TiffinStatus.ACTIVE)
                .hasBreakfast(dto.isHasBreakfast())
                .hasLunch(dto.isHasLunch())
                .hasDinner(dto.isHasDinner())
                .allDays(dto.isAllDays())
                .customDays(dto.isAllDays() ? null : dto.getCustomDaysString())
                .startDate(dto.getStartDate())
                .build();

        return subscriptionRepository.save(sub);
    }

    @Transactional
    public void update(Long id, TiffinSubscriptionDto dto) {
        TiffinSubscription sub = getById(id);
        sub.setHasBreakfast(dto.isHasBreakfast());
        sub.setHasLunch(dto.isHasLunch());
        sub.setHasDinner(dto.isHasDinner());
        sub.setAllDays(dto.isAllDays());
        sub.setCustomDays(dto.isAllDays() ? null : dto.getCustomDaysString());
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void pause(Long id, TiffinPauseDto dto) {
        TiffinSubscription sub = getById(id);
        TiffinPause pause = TiffinPause.builder()
                .subscription(sub)
                .pauseFrom(dto.getPauseFrom())
                .pauseUntil(dto.getPauseUntil())
                .note(dto.getNote())
                .build();
        pauseRepository.save(pause);

        if (!dto.getPauseFrom().isAfter(LocalDate.now())) {
            sub.setStatus(TiffinStatus.PAUSED);
            subscriptionRepository.save(sub);
        }
    }

    @Transactional
    public void resume(Long id) {
        TiffinSubscription sub = getById(id);
        sub.setStatus(TiffinStatus.ACTIVE);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void cancel(Long id) {
        TiffinSubscription sub = getById(id);
        sub.setStatus(TiffinStatus.CANCELLED);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void updatePricingOverride(Long id, TiffinPricingOverrideDto dto) {
        TiffinSubscription sub = getById(id);
        sub.setCustomMonthlyPrice(dto.getCustomMonthlyPrice());
        sub.setDeliveryCharge(dto.getDeliveryCharge());
        subscriptionRepository.save(sub);
    }

    public List<TiffinPricing> getAllPricing() {
        return pricingRepository.findAll();
    }

    public Map<String, BigDecimal> getPricingMap() {
        Map<String, BigDecimal> map = new java.util.LinkedHashMap<>();
        pricingRepository.findAll().forEach(p -> map.put(p.getMealType().name(), p.getPricePerMonth()));
        return map;
    }

    private Map<TiffinMealType, BigDecimal> getPricingByType() {
        Map<TiffinMealType, BigDecimal> map = new EnumMap<>(TiffinMealType.class);
        pricingRepository.findAll().forEach(p -> map.put(p.getMealType(), p.getPricePerMonth()));
        return map;
    }

    @Transactional
    public void updatePricing(TiffinPricingDto dto) {
        updateMealPrice(TiffinMealType.BREAKFAST, dto.getBreakfastPrice());
        updateMealPrice(TiffinMealType.LUNCH, dto.getLunchPrice());
        updateMealPrice(TiffinMealType.DINNER, dto.getDinnerPrice());
    }

    private void updateMealPrice(TiffinMealType mealType, BigDecimal price) {
        pricingRepository.findByMealType(mealType).ifPresent(p -> {
            p.setPricePerMonth(price);
            pricingRepository.save(p);
        });
    }

    public BigDecimal calculateMonthlyPrice(TiffinSubscription sub) {
        return calculateMonthlyPrice(sub, getPricingByType());
    }

    private BigDecimal calculateMonthlyPrice(TiffinSubscription sub, Map<TiffinMealType, BigDecimal> standardPrices) {
        return calculateMealPrice(sub, standardPrices).add(sub.getDeliveryCharge());
    }

    private BigDecimal calculateMealPrice(TiffinSubscription sub, Map<TiffinMealType, BigDecimal> standardPrices) {
        if (sub.getCustomMonthlyPrice() != null) {
            return sub.getCustomMonthlyPrice();
        }
        BigDecimal total = BigDecimal.ZERO;
        if (sub.isHasBreakfast()) total = total.add(standardPrices.getOrDefault(TiffinMealType.BREAKFAST, BigDecimal.ZERO));
        if (sub.isHasLunch())     total = total.add(standardPrices.getOrDefault(TiffinMealType.LUNCH,     BigDecimal.ZERO));
        if (sub.isHasDinner())    total = total.add(standardPrices.getOrDefault(TiffinMealType.DINNER,    BigDecimal.ZERO));
        return total;
    }

    public List<TiffinPause> getPausesForSubscription(Long subscriptionId) {
        return pauseRepository.findBySubscriptionIdOrderByPauseFromDesc(subscriptionId);
    }

    public long countActiveSubscriptions() {
        return subscriptionRepository.countByStatusIn(List.of(TiffinStatus.ACTIVE));
    }

    public BigDecimal calculateTotalMonthlyTiffinRevenue() {
        List<TiffinSubscription> active = subscriptionRepository.findByStatusInWithCustomer(
                List.of(TiffinStatus.ACTIVE));
        Map<TiffinMealType, BigDecimal> prices = getPricingByType();
        return active.stream()
                .map(sub -> calculateMonthlyPrice(sub, prices))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void recordPayment(Long subscriptionId, TiffinPaymentDto dto) {
        TiffinSubscription sub = getById(subscriptionId);
        TiffinPayment payment = TiffinPayment.builder()
                .subscription(sub)
                .amountPaid(dto.getAmountPaid())
                .coverageFrom(dto.getCoverageFrom())
                .coverageUntil(dto.getCoverageUntil())
                .paidOn(dto.getPaidOn())
                .note(dto.getNote())
                .build();
        paymentRepository.save(payment);
    }

    public List<TiffinPayment> getPaymentsForSubscription(Long subscriptionId) {
        return paymentRepository.findBySubscriptionIdOrderByCoverageFromDesc(subscriptionId);
    }

    public LocalDate getPaidThroughDate(Long subscriptionId) {
        return paymentRepository.findPaidThroughDate(subscriptionId).orElse(null);
    }

    public Map<Long, LocalDate> getPaidThroughMap(List<Long> subscriptionIds) {
        Map<Long, LocalDate> map = new HashMap<>();
        for (Object[] row : paymentRepository.findPaidThroughDates(subscriptionIds)) {
            map.put((Long) row[0], (LocalDate) row[1]);
        }
        return map;
    }

    public BigDecimal getTotalPaid(Long subscriptionId) {
        return paymentRepository.sumAmountPaidForSubscription(subscriptionId);
    }

    // Prefills the payment form's coverage-start with the day after the current
    // paid-through date, so consecutive payments naturally chain without gaps.
    public LocalDate getNextCoverageStart(TiffinSubscription sub) {
        LocalDate paidThrough = getPaidThroughDate(sub.getId());
        return paidThrough != null ? paidThrough.plusDays(1) : sub.getStartDate();
    }
}
