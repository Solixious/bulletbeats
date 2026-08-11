package in.bulletbeats.domain.offers.entity;

import in.bulletbeats.domain.offers.entity.enums.CohortRuleField;
import in.bulletbeats.domain.offers.entity.enums.CohortRuleOperator;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customer_cohort_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCohortRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    private CustomerCohort cohort;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private CohortRuleField field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private CohortRuleOperator operator;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    /** Only meaningful for TOTAL_SPEND_IN_PERIOD / VISITS_IN_PERIOD. */
    private Integer periodDays;
}
