package in.bulletbeats.domain.dashboard;

import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.repository.BillItemRepository;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.billing.repository.CafeTableRepository;
import in.bulletbeats.domain.dashboard.dto.DashboardStatsDto;
import in.bulletbeats.domain.dashboard.dto.TableStatusDto;
import in.bulletbeats.domain.dashboard.dto.TopItemDto;
import in.bulletbeats.domain.inventory.repository.PurchaseOrderRepository;
import in.bulletbeats.domain.inventory.repository.ReplenishmentRequestRepository;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.ReplenishmentStatus;
import in.bulletbeats.domain.shared.enums.TableStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final CafeTableRepository cafeTableRepository;
    private final InventoryService inventoryService;
    private final ReplenishmentRequestRepository replenishmentRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public DashboardStatsDto buildStats(boolean isManagerOrAdmin) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // Active operations (all roles)
        long activeBillCount = billRepository.countActiveBills();

        List<CafeTable> tables = cafeTableRepository.findAllActiveSorted();
        long occupiedCount = tables.stream()
                .filter(t -> t.getStatus() == TableStatus.OCCUPIED)
                .count();

        List<TableStatusDto> tableStatuses = tables.stream().map(t -> {
            List<Bill> tableBills = billRepository.findByCafeTableIdAndStatusIn(
                    t.getId(), List.of(BillStatus.DRAFT, BillStatus.CONFIRMED));
            String customerName = tableBills.stream()
                    .filter(b -> b.getCustomer() != null)
                    .map(b -> b.getCustomer().getName())
                    .findFirst()
                    .orElse(null);
            return new TableStatusDto(
                    t.getId(), t.getName(), t.getStatus(),
                    tableBills.size(), customerName);
        }).toList();

        if (!isManagerOrAdmin) {
            return DashboardStatsDto.staffView(
                    activeBillCount, occupiedCount, tables.size(), tableStatuses);
        }

        // Today stats
        BigDecimal todayRevenue = billRepository.getRevenueForDate(today);
        long todayBillCount = billRepository.getBillCountForDate(today);
        BigDecimal todayAov = todayBillCount > 0
                ? todayRevenue.divide(BigDecimal.valueOf(todayBillCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Monthly stats — 1st of month to now
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        BigDecimal monthRevenue = billRepository.getRevenueForRange(monthStart, todayEnd);
        long monthBillCount = billRepository.getBillCountForRange(monthStart, todayEnd);
        BigDecimal monthAov = monthBillCount > 0
                ? monthRevenue.divide(BigDecimal.valueOf(monthBillCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Last month — full calendar month
        LocalDate firstOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate firstOfThisMonth = today.withDayOfMonth(1);
        BigDecimal lastMonthRevenue = billRepository.getRevenueForRange(
                firstOfLastMonth.atStartOfDay(),
                firstOfThisMonth.atStartOfDay());

        // Last year YTD — Jan 1 to same day last year
        LocalDateTime lastYearStart = LocalDate.of(today.getYear() - 1, 1, 1).atStartOfDay();
        LocalDateTime lastYearEnd = today.minusYears(1).plusDays(1).atStartOfDay();
        BigDecimal lastYearYtdRevenue = billRepository.getRevenueForRange(lastYearStart, lastYearEnd);

        // Compute deltas
        BigDecimal vsLastMonthAmount = monthRevenue.subtract(lastMonthRevenue);
        BigDecimal vsLastMonthPercent = lastMonthRevenue.signum() > 0
                ? vsLastMonthAmount
                        .divide(lastMonthRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                : null;

        BigDecimal vsLastYearAmount = monthRevenue.subtract(lastYearYtdRevenue);
        BigDecimal vsLastYearPercent = lastYearYtdRevenue.signum() > 0
                ? vsLastYearAmount
                        .divide(lastYearYtdRevenue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                : null;

        // Grocery spend — POs marked as ordered in each month
        BigDecimal thisMonthGrocerySpend = purchaseOrderRepository
                .sumSpendForRange(monthStart, todayEnd);
        BigDecimal lastMonthGrocerySpend = purchaseOrderRepository
                .sumSpendForRange(firstOfLastMonth.atStartOfDay(), firstOfThisMonth.atStartOfDay());

        BigDecimal vsGrocerySpendAmount = thisMonthGrocerySpend.subtract(lastMonthGrocerySpend);
        BigDecimal vsGrocerySpendPercent = lastMonthGrocerySpend.signum() > 0
                ? vsGrocerySpendAmount
                        .divide(lastMonthGrocerySpend, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                : null;

        BigDecimal groceryBarMax = thisMonthGrocerySpend.max(lastMonthGrocerySpend);
        int groceryBarThisWidth;
        int groceryBarLastWidth;
        if (groceryBarMax.signum() == 0) {
            groceryBarThisWidth = 0;
            groceryBarLastWidth = 0;
        } else {
            groceryBarThisWidth = thisMonthGrocerySpend
                    .divide(groceryBarMax, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
            groceryBarLastWidth = lastMonthGrocerySpend
                    .divide(groceryBarMax, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
        }

        // Top items today
        List<Object[]> rawTop = billItemRepository.findTopItemsForDate(today, 5);
        List<TopItemDto> topItems = rawTop.stream()
                .map(row -> new TopItemDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        (BigDecimal) row[2]))
                .toList();

        // Low stock + replenishment
        long lowStockCount = inventoryService.getLowStockCount();
        long pendingReplenishmentCount =
                replenishmentRequestRepository.countByStatus(ReplenishmentStatus.PENDING);

        // Bar widths for monthly comparison (0-100)
        BigDecimal monthBarMax = monthRevenue.max(lastMonthRevenue);
        int thisMonthBarWidth;
        int lastMonthBarWidth;
        if (monthBarMax.signum() == 0) {
            thisMonthBarWidth = 0;
            lastMonthBarWidth = 0;
        } else {
            thisMonthBarWidth = monthRevenue
                    .divide(monthBarMax, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
            lastMonthBarWidth = lastMonthRevenue
                    .divide(monthBarMax, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
        }

        // Bar widths for year comparison (0-100)
        BigDecimal yearBarMax = monthRevenue.max(lastYearYtdRevenue);
        int thisYearBarWidth;
        int lastYearBarWidth;
        if (yearBarMax.signum() == 0) {
            thisYearBarWidth = 0;
            lastYearBarWidth = 0;
        } else {
            thisYearBarWidth = monthRevenue
                    .divide(yearBarMax, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
            lastYearBarWidth = lastYearYtdRevenue
                    .divide(yearBarMax, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).intValue();
        }

        return DashboardStatsDto.managerView(
                todayRevenue, todayBillCount, todayAov,
                monthRevenue, monthBillCount, monthAov,
                lastMonthRevenue, lastYearYtdRevenue,
                vsLastMonthAmount, vsLastMonthPercent,
                vsLastMonthAmount.signum() >= 0,
                vsLastYearAmount, vsLastYearPercent,
                vsLastYearAmount.signum() >= 0,
                topItems,
                lowStockCount, pendingReplenishmentCount,
                thisMonthBarWidth, lastMonthBarWidth,
                thisYearBarWidth, lastYearBarWidth,
                thisMonthGrocerySpend, lastMonthGrocerySpend,
                vsGrocerySpendAmount, vsGrocerySpendPercent,
                vsGrocerySpendAmount.signum() > 0,
                groceryBarThisWidth, groceryBarLastWidth,
                activeBillCount, occupiedCount,
                tables.size(), tableStatuses);
    }
}
