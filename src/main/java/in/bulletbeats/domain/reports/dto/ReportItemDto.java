package in.bulletbeats.domain.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReportItemDto {
    private Long itemId;
    private String itemName;
    private String categoryName;
    private long quantity;
    private BigDecimal revenue;
}
