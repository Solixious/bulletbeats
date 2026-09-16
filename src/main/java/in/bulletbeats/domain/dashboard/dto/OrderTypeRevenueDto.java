package in.bulletbeats.domain.dashboard.dto;

import in.bulletbeats.domain.shared.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderTypeRevenueDto {
    private OrderType orderType;
    private BigDecimal revenue;
    private BigDecimal percent;
    private int barWidth;
}
