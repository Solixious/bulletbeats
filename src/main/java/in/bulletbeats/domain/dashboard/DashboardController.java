package in.bulletbeats.domain.dashboard;

import in.bulletbeats.domain.dashboard.dto.DashboardStatsDto;
import in.bulletbeats.domain.reports.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private static final int TOP_N = 5;

    private final DashboardService dashboardService;
    private final ReportService reportService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")
                            || a.getAuthority().equals("ROLE_ADMIN"));

        DashboardStatsDto stats = dashboardService.buildStats(isManager);
        model.addAttribute("stats", stats);
        model.addAttribute("isManager", isManager);

        LocalDate today = LocalDate.now();
        model.addAttribute("pageDate",
                today.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH)));
        model.addAttribute("currentMonth",
                today.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH));
        model.addAttribute("currentYear", today.getYear());
        model.addAttribute("lastYear", today.getYear() - 1);

        if (isManager) {
            String defaultPeriod = YearMonth.now().toString();
            model.addAttribute("periodOptions", reportService.buildPeriodOptions());

            model.addAttribute("topByQuantityPeriod", defaultPeriod);
            model.addAttribute("topByQuantity", reportService.getTopSellers(defaultPeriod, TOP_N));

            model.addAttribute("topByRevenuePeriod", defaultPeriod);
            model.addAttribute("topByRevenue", reportService.getTopByRevenue(defaultPeriod, TOP_N));
        }

        return "dashboard/dashboard";
    }

    @GetMapping("/dashboard/top-quantity")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String topByQuantity(@RequestParam(defaultValue = ReportService.ALL_TIME) String period, Model model) {
        model.addAttribute("topByQuantity", reportService.getTopSellers(period, TOP_N));
        return "dashboard/dashboard :: topByQuantityResults";
    }

    @GetMapping("/dashboard/top-revenue")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String topByRevenue(@RequestParam(defaultValue = ReportService.ALL_TIME) String period, Model model) {
        model.addAttribute("topByRevenue", reportService.getTopByRevenue(period, TOP_N));
        return "dashboard/dashboard :: topByRevenueResults";
    }
}
