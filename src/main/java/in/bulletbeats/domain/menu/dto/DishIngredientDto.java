package in.bulletbeats.domain.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DishIngredientDto {

    private Long groceryItemId;

    private Long preparedItemId;

    @NotNull
    @DecimalMin("0.001")
    private BigDecimal quantityRequired;

    /**
     * Combined selector used by the ingredient picker, e.g. "GROCERY:5" or "PREPARED:3".
     * Splits into groceryItemId/preparedItemId on bind so the form only needs one dropdown.
     */
    public void setIngredientRef(String ref) {
        if (ref == null || ref.isBlank()) return;
        String[] parts = ref.split(":", 2);
        if (parts.length != 2) return;
        Long id = Long.valueOf(parts[1]);
        if ("GROCERY".equals(parts[0])) {
            this.groceryItemId = id;
        } else if ("PREPARED".equals(parts[0])) {
            this.preparedItemId = id;
        }
    }

    public String getIngredientRef() {
        if (groceryItemId != null) return "GROCERY:" + groceryItemId;
        if (preparedItemId != null) return "PREPARED:" + preparedItemId;
        return "";
    }
}
