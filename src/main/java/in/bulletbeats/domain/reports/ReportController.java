package in.bulletbeats.domain.reports;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("periodOptions", reportService.buildPeriodOptions());
        model.addAttribute("summary", reportService.getSummary());

        model.addAttribute("topSellersPeriod", ReportService.ALL_TIME);
        model.addAttribute("topSellers", reportService.getTopSellers(ReportService.ALL_TIME));

        model.addAttribute("slowMoversPeriod", ReportService.ALL_TIME);
        model.addAttribute("slowMovers", reportService.getSlowMovers(ReportService.ALL_TIME));

        model.addAttribute("notSoldPeriod", ReportService.ALL_TIME);
        model.addAttribute("notSold", reportService.getNotSold(ReportService.ALL_TIME));

        return "reports/reports";
    }

    @GetMapping("/reports/top-sellers")
    public String topSellers(@RequestParam(defaultValue = ReportService.ALL_TIME) String period, Model model) {
        model.addAttribute("topSellers", reportService.getTopSellers(period));
        return "reports/reports :: topSellersResults";
    }

    @GetMapping("/reports/slow-movers")
    public String slowMovers(@RequestParam(defaultValue = ReportService.ALL_TIME) String period, Model model) {
        model.addAttribute("slowMovers", reportService.getSlowMovers(period));
        return "reports/reports :: slowMoversResults";
    }

    @GetMapping("/reports/not-sold")
    public String notSold(@RequestParam(defaultValue = ReportService.ALL_TIME) String period, Model model) {
        model.addAttribute("notSold", reportService.getNotSold(period));
        return "reports/reports :: notSoldResults";
    }
}
