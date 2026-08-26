package in.bulletbeats.domain.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PreparedItemIngredientDto {

    @NotNull
    private Long groceryItemId;

    @NotNull
    @DecimalMin("0.001")
    private BigDecimal quantityRequired;
}
