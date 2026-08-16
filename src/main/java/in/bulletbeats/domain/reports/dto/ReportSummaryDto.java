package in.bulletbeats.domain.reports.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReportSummaryDto {
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueAllTime;
    private long activeMenuItemCount;
    private long neverSoldAllTimeCount;
    private String monthLabel;
}
