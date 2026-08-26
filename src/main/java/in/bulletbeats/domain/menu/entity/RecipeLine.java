package in.bulletbeats.domain.menu.entity;

import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.entity.PreparedItem;

import java.math.BigDecimal;

/**
 * A single recipe line shared by DishIngredient and ComboIngredient: consumes either a
 * grocery item or a prepared item (in-house batch-prepped ingredient), never both.
 */
public interface RecipeLine {
    GroceryItem getGroceryItem();
    PreparedItem getPreparedItem();
    BigDecimal getQuantityRequired();
}
