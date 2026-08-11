package in.bulletbeats.domain.offers.entity;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.offers.entity.enums.CodeUsageType;
import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "offer_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @Column(nullable = false, length = 40, unique = true)
    private String code;

    /** Restricts redemption to one customer — used for single-customer compensation codes. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private CodeUsageType usageType;

    private Integer maxUses;

    @Column(nullable = false)
    @Builder.Default
    private int usesCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private LocalDateTime expiresAt;

    public boolean isExhausted() {
        return maxUses != null && usesCount >= maxUses;
    }
}
