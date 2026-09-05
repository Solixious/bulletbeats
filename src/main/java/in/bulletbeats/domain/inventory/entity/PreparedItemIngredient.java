package in.bulletbeats.domain.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "prepared_item_ingredients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparedItemIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepared_item_id", nullable = false)
    private PreparedItem preparedItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grocery_item_id")
    private GroceryItem groceryItem;

    /** Another prepared item used as an ingredient here, e.g. a filling used inside a frozen dumpling. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ingredient_prepared_item_id")
    private PreparedItem ingredientPreparedItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantityRequired;
}
