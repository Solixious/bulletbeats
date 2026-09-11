package in.bulletbeats.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderNameStatsDto {
    private long namedOrderCount;
    private long anonymousOrderCount;
    private BigDecimal namedOrderPercent;
    private BigDecimal anonymousOrderPercent;
}
