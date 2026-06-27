package in.bulletbeats.domain.inventory.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "grocery_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroceryItem extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityInStock;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal minThreshold;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal reorderQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_supplier_id")
    private Supplier defaultSupplier;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Long tenantId;

    @Version
    private Long version;

    public boolean isLowStock() {
        return quantityInStock != null && minThreshold != null
                && quantityInStock.compareTo(minThreshold) <= 0;
    }
}
