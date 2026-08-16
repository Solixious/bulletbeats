package in.bulletbeats.domain.reports;

import in.bulletbeats.domain.billing.repository.BillItemRepository;
import in.bulletbeats.domain.billing.repository.BillRepository;
import in.bulletbeats.domain.menu.repository.MenuItemRepository;
import in.bulletbeats.domain.reports.dto.MenuItemSalesSummaryDto;
import in.bulletbeats.domain.reports.dto.ReportDto;
import in.bulletbeats.domain.reports.dto.ReportItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final int TOP_N = 15;
    private static final int SLOW_MOVER_N = 10;

    private final BillItemRepository billItemRepository;
    private final BillRepository billRepository;
    private final MenuItemRepository menuItemRepository;

    public ReportDto buildReport() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        List<ReportItemDto> topThisMonth = billItemRepository
                .findTopItemsForRangeGrouped(monthStart, todayEnd, TOP_N).stream()
                .map(this::toReportItem)
                .toList();

        List<MenuItemSalesSummaryDto> allActiveItems = menuItemRepository.findAllActiveItemsSalesSummary().stream()
                .map(this::toSalesSummary)
                .toList();

        List<MenuItemSalesSummaryDto> soldAtLeastOnce = allActiveItems.stream()
                .filter(item -> item.getQuantity() > 0)
                .sorted(Comparator.comparingLong(MenuItemSalesSummaryDto::getQuantity).reversed())
                .toList();

        List<ReportItemDto> topAllTime = soldAtLeastOnce.stream()
                .limit(TOP_N)
                .map(item -> new ReportItemDto(
                        item.getItemId(), item.getItemName(), item.getCategoryName(),
                        item.getQuantity(), item.getRevenue()))
                .toList();

        List<MenuItemSalesSummaryDto> deadStock = allActiveItems.stream()
                .filter(item -> item.getQuantity() == 0)
                .sorted(Comparator.comparing(MenuItemSalesSummaryDto::getItemName))
                .toList();

        List<MenuItemSalesSummaryDto> slowMovers = soldAtLeastOnce.stream()
                .sorted(Comparator.comparingLong(MenuItemSalesSummaryDto::getQuantity))
                .limit(SLOW_MOVER_N)
                .toList();

        BigDecimal revenueThisMonth = billRepository.getRevenueForRange(monthStart, todayEnd);
        BigDecimal revenueAllTime = billRepository.getTotalRevenueAllTime();

        String monthLabel = today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + today.getYear();

        return new ReportDto(
                topThisMonth, topAllTime, deadStock, slowMovers,
                revenueThisMonth, revenueAllTime,
                allActiveItems.size(), deadStock.size(), monthLabel);
    }

    private ReportItemDto toReportItem(Object[] row) {
        return new ReportItemDto(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue(),
                (BigDecimal) row[4]);
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
