package in.bulletbeats.domain.dashboard;

import in.bulletbeats.domain.dashboard.dto.DashboardStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

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

        return "dashboard/dashboard";
    }
}
