package in.bulletbeats.domain.tiffin.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tiffin_payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TiffinPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private TiffinSubscription subscription;

    @Column(name = "amount_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(nullable = false)
    private LocalDate coverageFrom;

    @Column(nullable = false)
    private LocalDate coverageUntil;

    @Column(nullable = false)
    private LocalDate paidOn;

    @Column(length = 255)
    private String note;
}
