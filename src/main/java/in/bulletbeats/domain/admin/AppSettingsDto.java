package in.bulletbeats.domain.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppSettingsDto {

    @NotBlank
    private String cafeName;

    private String cafeAddress;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal gstRate;

    private boolean gstInclusive;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal loyaltyEarnRate;

    private String appBaseUrl;

    @Min(1)
    private int idleTimeoutMinutes = 10;
}
