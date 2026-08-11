package in.bulletbeats.domain.offers.service;

import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.offers.entity.CustomerCohort;
import in.bulletbeats.domain.offers.entity.CustomerCohortRule;
import in.bulletbeats.domain.offers.entity.enums.CohortRuleOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class CohortEvaluationService {

    private final BillRepository billRepository;

    public boolean matches(CustomerCohort cohort, Customer customer) {
        if (customer == null) {
            return cohort.getRules().isEmpty();
        }
        return cohort.getRules().stream().allMatch(rule -> matches(rule, customer));
    }

    public boolean matches(CustomerCohortRule rule, Customer customer) {
        BigDecimal actual = switch (rule.getField()) {
            case LOYALTY_POINTS -> BigDecimal.valueOf(customer.getLoyaltyPoints());
            case TOTAL_VISITS -> BigDecimal.valueOf(customer.getVisitCount());
            case TOTAL_SPEND -> customer.getTotalSpend();
            case TOTAL_SPEND_IN_PERIOD -> billRepository.sumTotalAmountByCustomerIdAndCreatedAtAfter(
                    customer.getId(), sinceDays(rule.getPeriodDays()));
            case VISITS_IN_PERIOD -> BigDecimal.valueOf(billRepository.countByCustomerIdAndCreatedAtAfter(
                    customer.getId(), sinceDays(rule.getPeriodDays())));
            case LAST_VISIT_DAYS_AGO -> customer.getLastVisitDate() == null
                    ? BigDecimal.valueOf(Long.MAX_VALUE)
                    : BigDecimal.valueOf(ChronoUnit.DAYS.between(customer.getLastVisitDate(), LocalDateTime.now()));
            case IS_STUDENT -> customer.isStudent() ? BigDecimal.ONE : BigDecimal.ZERO;
            case IS_VIP -> customer.isVip() ? BigDecimal.ONE : BigDecimal.ZERO;
        };
        return compare(actual, rule.getOperator(), rule.getValue());
    }

    private LocalDateTime sinceDays(Integer periodDays) {
        int days = periodDays != null ? periodDays : 30;
        return LocalDateTime.now().minusDays(days);
    }

    private boolean compare(BigDecimal actual, CohortRuleOperator operator, BigDecimal expected) {
        int cmp = actual.compareTo(expected);
        return switch (operator) {
            case GT -> cmp > 0;
            case GTE -> cmp >= 0;
            case LT -> cmp < 0;
            case LTE -> cmp <= 0;
            case EQ -> cmp == 0;
        };
    }
}
