package in.bulletbeats.domain.tiffin.entity;

import in.bulletbeats.domain.shared.BaseEntity;
import in.bulletbeats.domain.tiffin.TiffinMealType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tiffin_pricing")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TiffinPricing extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    private TiffinMealType mealType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerMonth;
}
