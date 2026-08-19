package in.bulletbeats.domain.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UnitConversionDto {
    private Long fromUnitId;
    private Long toUnitId;
    private BigDecimal factor;
}
