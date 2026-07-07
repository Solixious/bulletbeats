package in.bulletbeats.domain.tiffin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TiffinPricingDto {

    @NotNull
    @DecimalMin(value = "0", message = "Price cannot be negative")
    private BigDecimal breakfastPrice = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0", message = "Price cannot be negative")
    private BigDecimal lunchPrice = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0", message = "Price cannot be negative")
    private BigDecimal dinnerPrice = BigDecimal.ZERO;
}
