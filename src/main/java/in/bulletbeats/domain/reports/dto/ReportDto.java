package in.bulletbeats.domain.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ReportDto {
    private List<ReportItemDto> topItemsThisMonth;
    private List<ReportItemDto> topItemsAllTime;
    private List<MenuItemSalesSummaryDto> deadStockItems;
    private List<MenuItemSalesSummaryDto> slowMovers;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueAllTime;
    private int activeMenuItemCount;
    private int deadStockCount;
    private String monthLabel;
}
