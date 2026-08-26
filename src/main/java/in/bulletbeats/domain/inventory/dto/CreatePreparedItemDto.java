package in.bulletbeats.domain.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePreparedItemDto {

    @NotBlank
    @Size(max = 150)
    private String name;

    private String description;

    private Integer prepTimeMinutes;

    @NotBlank
    @Size(max = 30)
    private String unit;

    @Size(max = 30)
    private String minorUnit;

    @NotNull
    @DecimalMin("0.001")
    private BigDecimal batchYieldQuantity;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal minThreshold;

    @Valid
    private List<PreparedItemIngredientDto> ingredients;
}
