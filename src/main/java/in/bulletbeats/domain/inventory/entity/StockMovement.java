package in.bulletbeats.domain.inventory.entity;

import in.bulletbeats.domain.shared.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grocery_item_id", nullable = false)
    private GroceryItem groceryItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType movementType;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal stockBefore;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal stockAfter;

    @Column(length = 30)
    private String referenceType;

    private Long referenceId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Long createdBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
