package in.bulletbeats.domain.inventory.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "unit_conversions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitConversion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_unit_id", nullable = false)
    private Unit fromUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_unit_id", nullable = false)
    private Unit toUnit;

    /** How many "toUnit" make up 1 "fromUnit", e.g. fromUnit=kg, toUnit=g, factor=1000. */
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal factor;
}
