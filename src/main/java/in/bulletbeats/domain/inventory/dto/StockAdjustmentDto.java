package in.bulletbeats.domain.inventory.dto;

import in.bulletbeats.domain.shared.enums.MovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockAdjustmentDto {

    @NotNull
    private MovementType movementType;

    @NotNull
    @DecimalMin("0.001")
    private BigDecimal quantity;

    private String notes;
}
