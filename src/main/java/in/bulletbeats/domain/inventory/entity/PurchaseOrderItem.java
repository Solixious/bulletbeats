package in.bulletbeats.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order_items")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grocery_item_id", nullable = false)
    private GroceryItem groceryItem;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityOrdered;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityReceived;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
