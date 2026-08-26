package in.bulletbeats.domain.menu.entity;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.PreparedItem;
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
public class DishIngredient implements RecipeLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grocery_item_id")
    private GroceryItem groceryItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prepared_item_id")
    private PreparedItem preparedItem;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantityRequired;
}
