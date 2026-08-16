package in.bulletbeats.domain.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MenuItemSalesSummaryDto {
    private Long itemId;
    private String itemName;
    private String categoryName;
    private BigDecimal price;
    private long quantity;
    private BigDecimal revenue;
    private LocalDateTime lastSoldAt;
}
