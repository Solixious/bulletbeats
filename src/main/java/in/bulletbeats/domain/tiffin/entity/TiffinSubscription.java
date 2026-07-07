package in.bulletbeats.domain.tiffin.entity;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.shared.BaseEntity;
import in.bulletbeats.domain.tiffin.TiffinStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tiffin_subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TiffinSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private TiffinStatus status = TiffinStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private boolean hasBreakfast = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean hasLunch = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean hasDinner = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean allDays = true;

    @Column(length = 50)
    private String customDays;

    @Column(nullable = false)
    private LocalDate startDate;
}
