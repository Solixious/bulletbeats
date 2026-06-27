package in.bulletbeats.domain.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardStatsDto {

    // Today stats (Manager/Admin only)
    private final BigDecimal todayRevenue;
    private final long todayBillCount;
    private final BigDecimal todayAov;

    // Monthly stats (Manager/Admin only)
    private final BigDecimal monthRevenue;
    private final long monthBillCount;
    private final BigDecimal monthAov;

    // Comparisons (Manager/Admin only)
    private final BigDecimal lastMonthRevenue;
    private final BigDecimal lastYearYtdRevenue;

    // Derived comparison fields
    private final BigDecimal vsLastMonthAmount;
    private final BigDecimal vsLastMonthPercent;
    private final boolean vsLastMonthPositive;
    private final BigDecimal vsLastYearAmount;
    private final BigDecimal vsLastYearPercent;
    private final boolean vsLastYearPositive;

    // Top selling items today (Manager/Admin only)
    private final List<TopItemDto> topItemsToday;

    // Low stock (Manager/Admin only)
    private final long lowStockCount;
    private final long pendingReplenishmentCount;

    // Bar widths for comparison charts (0-100, computed in service)
    private final int thisMonthBarWidth;
    private final int lastMonthBarWidth;
    private final int thisYearBarWidth;
    private final int lastYearBarWidth;

    // Grocery spend (Manager/Admin only)
    private final BigDecimal thisMonthGrocerySpend;
    private final BigDecimal lastMonthGrocerySpend;
    private final BigDecimal vsGrocerySpendAmount;
    private final BigDecimal vsGrocerySpendPercent;
    private final boolean vsGrocerySpendUp;
    private final int groceryBarThisWidth;
    private final int groceryBarLastWidth;

    // Active operations (all roles)
    private final long activeBillCount;
    private final long occupiedTableCount;
    private final long totalTableCount;
    private final List<TableStatusDto> tableStatuses;

    public static DashboardStatsDto staffView(
            long activeBillCount,
            long occupiedTableCount,
            long totalTableCount,
            List<TableStatusDto> tableStatuses) {
        return DashboardStatsDto.builder()
                .activeBillCount(activeBillCount)
                .occupiedTableCount(occupiedTableCount)
                .totalTableCount(totalTableCount)
                .tableStatuses(tableStatuses)
                .build();
    }

    public static DashboardStatsDto managerView(
            BigDecimal todayRevenue,
            long todayBillCount,
            BigDecimal todayAov,
            BigDecimal monthRevenue,
            long monthBillCount,
            BigDecimal monthAov,
            BigDecimal lastMonthRevenue,
            BigDecimal lastYearYtdRevenue,
            BigDecimal vsLastMonthAmount,
            BigDecimal vsLastMonthPercent,
            boolean vsLastMonthPositive,
            BigDecimal vsLastYearAmount,
            BigDecimal vsLastYearPercent,
            boolean vsLastYearPositive,
            List<TopItemDto> topItemsToday,
            long lowStockCount,
            long pendingReplenishmentCount,
            int thisMonthBarWidth,
            int lastMonthBarWidth,
            int thisYearBarWidth,
            int lastYearBarWidth,
            BigDecimal thisMonthGrocerySpend,
            BigDecimal lastMonthGrocerySpend,
            BigDecimal vsGrocerySpendAmount,
            BigDecimal vsGrocerySpendPercent,
            boolean vsGrocerySpendUp,
            int groceryBarThisWidth,
            int groceryBarLastWidth,
            long activeBillCount,
            long occupiedTableCount,
            long totalTableCount,
            List<TableStatusDto> tableStatuses) {
        return DashboardStatsDto.builder()
                .todayRevenue(todayRevenue)
                .todayBillCount(todayBillCount)
                .todayAov(todayAov)
                .monthRevenue(monthRevenue)
                .monthBillCount(monthBillCount)
                .monthAov(monthAov)
                .lastMonthRevenue(lastMonthRevenue)
                .lastYearYtdRevenue(lastYearYtdRevenue)
                .vsLastMonthAmount(vsLastMonthAmount)
                .vsLastMonthPercent(vsLastMonthPercent)
                .vsLastMonthPositive(vsLastMonthPositive)
                .vsLastYearAmount(vsLastYearAmount)
                .vsLastYearPercent(vsLastYearPercent)
                .vsLastYearPositive(vsLastYearPositive)
                .topItemsToday(topItemsToday)
                .lowStockCount(lowStockCount)
                .pendingReplenishmentCount(pendingReplenishmentCount)
                .thisMonthBarWidth(thisMonthBarWidth)
                .lastMonthBarWidth(lastMonthBarWidth)
                .thisYearBarWidth(thisYearBarWidth)
                .lastYearBarWidth(lastYearBarWidth)
                .thisMonthGrocerySpend(thisMonthGrocerySpend)
                .lastMonthGrocerySpend(lastMonthGrocerySpend)
                .vsGrocerySpendAmount(vsGrocerySpendAmount)
                .vsGrocerySpendPercent(vsGrocerySpendPercent)
                .vsGrocerySpendUp(vsGrocerySpendUp)
                .groceryBarThisWidth(groceryBarThisWidth)
                .groceryBarLastWidth(groceryBarLastWidth)
                .activeBillCount(activeBillCount)
                .occupiedTableCount(occupiedTableCount)
                .totalTableCount(totalTableCount)
                .tableStatuses(tableStatuses)
                .build();
    }
}
