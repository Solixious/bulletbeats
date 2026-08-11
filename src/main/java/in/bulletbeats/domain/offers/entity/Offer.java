package in.bulletbeats.domain.offers.entity;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.offers.entity.enums.OfferMechanism;
import in.bulletbeats.domain.offers.entity.enums.OfferTargetType;
import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferMechanism mechanism;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentageValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal fixedValue;

    /** Only for BUY_X_GET_Y_FREE. */
    private Integer buyQuantity;

    private Integer getQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private OfferTargetType targetType = OfferTargetType.ALL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id")
    private CustomerCohort cohort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minSpend = BigDecimal.ZERO;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean requiresCode = false;

    private Integer maxTotalUses;

    private Integer maxUsesPerCustomer;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean legacyStudentDiscount = false;

    @Column(nullable = false)
    @Builder.Default
    private long tenantId = 1L;

    @Builder.Default
    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferItemScope> items = new ArrayList<>();

    public boolean isWithinWindow(LocalDateTime at) {
        if (startsAt != null && at.isBefore(startsAt)) return false;
        if (endsAt != null && at.isAfter(endsAt)) return false;
        return true;
    }

    public boolean isItemScoped() {
        return mechanism == OfferMechanism.ITEM_PERCENTAGE
                || mechanism == OfferMechanism.ITEM_FIXED
                || mechanism == OfferMechanism.BUY_X_GET_Y_FREE;
    }
}
