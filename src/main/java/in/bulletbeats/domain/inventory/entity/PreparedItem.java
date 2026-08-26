package in.bulletbeats.domain.inventory.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prepared_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparedItem extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer prepTimeMinutes;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(name = "minor_unit", length = 30)
    private String minorUnit;

    @Column(name = "batch_yield_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal batchYieldQuantity;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityInStock;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal minThreshold;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private Long tenantId;

    @Version
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "preparedItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PreparedItemIngredient> ingredients = new ArrayList<>();

    public boolean isLowStock() {
        return quantityInStock != null && minThreshold != null
                && quantityInStock.compareTo(minThreshold) < 0;
    }

    /** Unit that dish/combo recipe quantities are expressed in: the minor unit if configured, else the stock unit. */
    public String getRecipeUnit() {
        return minorUnit != null ? minorUnit : unit;
    }
}
