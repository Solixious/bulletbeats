package in.bulletbeats.domain.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateGroceryItemDto {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 30)
    private String unit;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal minThreshold;

    @NotNull
    @DecimalMin("0.1")
    private BigDecimal reorderQuantity;

    private Long supplierId;
}
