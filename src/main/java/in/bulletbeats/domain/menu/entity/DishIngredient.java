package in.bulletbeats.domain.menu.entity;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "dish_ingredients")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grocery_item_id", nullable = false)
    private GroceryItem groceryItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantityRequired;
}
