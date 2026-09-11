package in.bulletbeats.domain.dashboard;

import in.bulletbeats.domain.billing.entity.Bill;
import in.bulletbeats.domain.billing.entity.CafeTable;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.billing.repository.CafeTableRepository;
import in.bulletbeats.domain.dashboard.dto.DailyRevenueDto;
import in.bulletbeats.domain.dashboard.dto.DashboardStatsDto;
import in.bulletbeats.domain.dashboard.dto.OrderNameStatsDto;
import in.bulletbeats.domain.dashboard.dto.TableStatusDto;
import in.bulletbeats.domain.inventory.repository.PurchaseOrderRepository;
import in.bulletbeats.domain.inventory.repository.ReplenishmentRequestRepository;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.tiffin.service.TiffinService;
import in.bulletbeats.domain.shared.enums.BillStatus;
import in.bulletbeats.domain.shared.enums.ReplenishmentStatus;
import in.bulletbeats.domain.shared.enums.TableStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final BillRepository billRepository;
    private final CafeTableRepository cafeTableRepository;
    private final InventoryService inventoryService;
    private final ReplenishmentRequestRepository replenishmentRequestRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final TiffinService tiffinService;

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

        // Tiffin monthly revenue
        BigDecimal tiffinMonthlyRevenue = tiffinService.calculateTotalMonthlyTiffinRevenue();
        long tiffinActiveCount = tiffinService.countActiveSubscriptions();

        // Customer retention — past full calendar week (Mon-Sun) vs current week-to-date
        LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate pastWeekStart = currentWeekStart.minusWeeks(1);
        LocalDate pastWeekEnd = currentWeekStart.minusDays(1);
        LocalDateTime currentWeekStartDt = currentWeekStart.atStartOfDay();
        LocalDateTime pastWeekStartDt = pastWeekStart.atStartOfDay();

        List<Long> pastWeekVisitorIds =
                billRepository.findDistinctPaidCustomerIdsInRange(pastWeekStartDt, currentWeekStartDt);
        Set<Long> currentWeekVisitorIds = new HashSet<>(
                billRepository.findDistinctPaidCustomerIdsInRange(currentWeekStartDt, todayEnd));

        Map<Long, LocalDateTime> firstVisitByCustomer = pastWeekVisitorIds.isEmpty()
                ? Map.of()
                : billRepository.findFirstPaidVisitDates(pastWeekVisitorIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (LocalDateTime) row[1]));

        long newCustomersPastWeek = 0;
        long returningCustomersPastWeek = 0;
        long newCustomerRetainedCount = 0;
        long returningCustomerRetainedCount = 0;

        for (Long customerId : pastWeekVisitorIds) {
            LocalDateTime firstVisit = firstVisitByCustomer.get(customerId);
            boolean wasNew = firstVisit != null && !firstVisit.isBefore(pastWeekStartDt);
            boolean returnedThisWeek = currentWeekVisitorIds.contains(customerId);
            if (wasNew) {
                newCustomersPastWeek++;
                if (returnedThisWeek) {
                    newCustomerRetainedCount++;
                }
            } else {
                returningCustomersPastWeek++;
                if (returnedThisWeek) {
                    returningCustomerRetainedCount++;
                }
            }
        }

        BigDecimal newCustomerRetentionRate = newCustomersPastWeek > 0
                ? BigDecimal.valueOf(newCustomerRetainedCount)
                        .divide(BigDecimal.valueOf(newCustomersPastWeek), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                : null;

        BigDecimal returningCustomerRetentionRate = returningCustomersPastWeek > 0
                ? BigDecimal.valueOf(returningCustomerRetainedCount)
                        .divide(BigDecimal.valueOf(returningCustomersPastWeek), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP)
                : null;

        // Named (has a customer) vs anonymous orders — past week, past month, all-time
        LocalDateTime last7DaysStart = today.minusDays(6).atStartOfDay();
        LocalDateTime last30DaysStart = today.minusDays(29).atStartOfDay();

        OrderNameStatsDto namedOrdersLastWeek = buildOrderNameStats(
                billRepository.countNamedOrdersForRange(last7DaysStart, todayEnd),
                billRepository.countAnonymousOrdersForRange(last7DaysStart, todayEnd));
        OrderNameStatsDto namedOrdersLastMonth = buildOrderNameStats(
                billRepository.countNamedOrdersForRange(last30DaysStart, todayEnd),
                billRepository.countAnonymousOrdersForRange(last30DaysStart, todayEnd));
        OrderNameStatsDto namedOrdersAllTime = buildOrderNameStats(
                billRepository.countNamedOrdersAllTime(),
                billRepository.countAnonymousOrdersAllTime());

        // Daily revenue for the past up to 30 days
        LocalDate rangeStart = today.minusDays(29);
        List<Object[]> rawDaily = billRepository.getDailyRevenueForRange(rangeStart.atStartOfDay(), todayEnd);
        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        rawDaily.forEach(row -> {
            LocalDate date = row[0] instanceof LocalDate d ? d : ((java.sql.Date) row[0]).toLocalDate();
            revenueByDate.put(date, (BigDecimal) row[1]);
        });

        BigDecimal dailyRevenueMax = revenueByDate.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<DailyRevenueDto> dailyRevenue = rangeStart.datesUntil(today.plusDays(1))
                .map(date -> {
                    BigDecimal revenue = revenueByDate.getOrDefault(date, BigDecimal.ZERO);
                    int barHeightPercent = dailyRevenueMax.signum() == 0
                            ? 0
                            : revenue.divide(dailyRevenueMax, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).intValue();
                    return new DailyRevenueDto(date, revenue, barHeightPercent);
                })
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
                dailyRevenue,
                lowStockCount, pendingReplenishmentCount,
                thisMonthBarWidth, lastMonthBarWidth,
                thisYearBarWidth, lastYearBarWidth,
                thisMonthGrocerySpend, lastMonthGrocerySpend,
                vsGrocerySpendAmount, vsGrocerySpendPercent,
                vsGrocerySpendAmount.signum() > 0,
                groceryBarThisWidth, groceryBarLastWidth,
                tiffinMonthlyRevenue, tiffinActiveCount,
                pastWeekStart, pastWeekEnd, currentWeekStart,
                newCustomersPastWeek, returningCustomersPastWeek,
                newCustomerRetainedCount, newCustomerRetentionRate,
                returningCustomerRetainedCount, returningCustomerRetentionRate,
                namedOrdersLastWeek, namedOrdersLastMonth, namedOrdersAllTime,
                activeBillCount, occupiedCount,
                tables.size(), tableStatuses);
    }

    private OrderNameStatsDto buildOrderNameStats(long namedCount, long anonymousCount) {
        long total = namedCount + anonymousCount;
        BigDecimal namedPercent = null;
        BigDecimal anonymousPercent = null;
        if (total > 0) {
            namedPercent = BigDecimal.valueOf(namedCount)
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
            anonymousPercent = BigDecimal.valueOf(100).subtract(namedPercent);
        }
        return new OrderNameStatsDto(namedCount, anonymousCount, namedPercent, anonymousPercent);
    }
}
