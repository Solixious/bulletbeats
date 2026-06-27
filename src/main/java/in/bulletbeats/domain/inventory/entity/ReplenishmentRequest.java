package in.bulletbeats.domain.inventory.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import in.bulletbeats.domain.shared.enums.ReplenishmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "replenishment_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplenishmentRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grocery_item_id", nullable = false)
    private GroceryItem groceryItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReplenishmentStatus status;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal requestedQty;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Long tenantId;
}
