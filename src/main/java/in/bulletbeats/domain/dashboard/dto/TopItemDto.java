package in.bulletbeats.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TopItemDto {
    private String itemName;
    private long quantityToday;
    private BigDecimal revenueToday;
}
