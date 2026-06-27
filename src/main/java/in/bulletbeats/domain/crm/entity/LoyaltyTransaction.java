package in.bulletbeats.domain.crm.entity;

import in.bulletbeats.domain.shared.enums.LoyaltyTransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private int points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoyaltyTransactionType transactionType;

    private Long billId;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
