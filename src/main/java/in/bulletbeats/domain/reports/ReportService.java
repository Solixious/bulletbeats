package in.bulletbeats.domain.reports;

import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.menu.repository.MenuItemRepository;
import in.bulletbeats.domain.reports.dto.MenuItemSalesSummaryDto;
import in.bulletbeats.domain.reports.dto.PeriodOptionDto;
import in.bulletbeats.domain.reports.dto.ReportSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    public static final String ALL_TIME = "ALL";
    private static final int PERIOD_MONTHS_BACK = 11;
    private static final int TOP_N = 15;
    private static final int SLOW_MOVER_N = 10;

    private final BillRepository billRepository;
    private final MenuItemRepository menuItemRepository;

    public List<PeriodOptionDto> buildPeriodOptions() {
        List<PeriodOptionDto> options = new ArrayList<>();
        options.add(new PeriodOptionDto(ALL_TIME, "All Time"));
        YearMonth current = YearMonth.now();
        for (int i = 0; i <= PERIOD_MONTHS_BACK; i++) {
            YearMonth ym = current.minusMonths(i);
            options.add(new PeriodOptionDto(ym.toString(), monthLabel(ym)));
        }
        return options;
    }

    public ReportSummaryDto getSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        BigDecimal revenueThisMonth = billRepository.getRevenueForRange(monthStart, todayEnd);
        BigDecimal revenueAllTime = billRepository.getTotalRevenueAllTime();
        long activeMenuItemCount = menuItemRepository.countByIsActiveTrue();
        long neverSoldAllTimeCount = fetchSummary(ALL_TIME).stream()
                .filter(item -> item.getQuantity() == 0)
                .count();

        return new ReportSummaryDto(
                revenueThisMonth, revenueAllTime, activeMenuItemCount, neverSoldAllTimeCount,
                monthLabel(YearMonth.from(today)));
    }

    public List<MenuItemSalesSummaryDto> getTopSellers(String period) {
        return fetchSummary(period).stream()
                .filter(item -> item.getQuantity() > 0)
                .sorted(Comparator.comparingLong(MenuItemSalesSummaryDto::getQuantity).reversed())
                .limit(TOP_N)
                .toList();
    }

    public List<MenuItemSalesSummaryDto> getSlowMovers(String period) {
        return fetchSummary(period).stream()
                .filter(item -> item.getQuantity() > 0)
                .sorted(Comparator.comparingLong(MenuItemSalesSummaryDto::getQuantity))
                .limit(SLOW_MOVER_N)
                .toList();
    }

    public List<MenuItemSalesSummaryDto> getNotSold(String period) {
        return fetchSummary(period).stream()
                .filter(item -> item.getQuantity() == 0)
                .sorted(Comparator.comparing(MenuItemSalesSummaryDto::getItemName))
                .toList();
    }

    private List<MenuItemSalesSummaryDto> fetchSummary(String period) {
        LocalDateTime from = null;
        LocalDateTime to = null;
        if (!ALL_TIME.equalsIgnoreCase(period)) {
            YearMonth ym = YearMonth.parse(period);
            from = ym.atDay(1).atStartOfDay();
            to = ym.plusMonths(1).atDay(1).atStartOfDay();
        }
        return menuItemRepository.findActiveItemsSalesSummary(from, to).stream()
                .map(this::toSalesSummary)
                .toList();
    }

    private String monthLabel(YearMonth ym) {
        return ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear();
    }

    private MenuItemSalesSummaryDto toSalesSummary(Object[] row) {
        return new MenuItemSalesSummaryDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (BigDecimal) row[3],
                ((Number) row[4]).longValue(),
                (BigDecimal) row[5],
                toLocalDateTime(row[6]));
    }

    private LocalDateTime toLocalDateTime(Object value) {
        return switch (value) {
            case null -> null;
            case LocalDateTime dt -> dt;
            case Timestamp ts -> ts.toLocalDateTime();
            case Instant instant -> LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            default -> throw new IllegalArgumentException(
                    "Unexpected type for lastSoldAt: " + value.getClass());
        };
    }
}
