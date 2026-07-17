package in.bulletbeats.domain.tiffin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TiffinPricingOverrideDto {

    // Null means fall back to standard meal pricing.
    @DecimalMin(value = "0", message = "Price cannot be negative")
    private BigDecimal customMonthlyPrice;

    @NotNull
    @DecimalMin(value = "0", message = "Delivery charge cannot be negative")
    private BigDecimal deliveryCharge = BigDecimal.ZERO;
}
