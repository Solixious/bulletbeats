package in.bulletbeats.domain.reports;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("report", reportService.buildReport());
        return "reports/reports";
    }
}
