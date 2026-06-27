package in.bulletbeats.domain.menu.entity;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "combo_ingredients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grocery_item_id", nullable = false)
    private GroceryItem groceryItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantityRequired;
}
