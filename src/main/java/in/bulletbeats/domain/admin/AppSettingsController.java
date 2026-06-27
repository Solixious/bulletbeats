package in.bulletbeats.domain.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Set;

@Controller
@RequestMapping("/admin/settings")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AppSettingsController {

    private final AppConfigService appConfigService;

    @GetMapping
    public String settings(Model model) {
        model.addAttribute("dto", loadDto());
        model.addAttribute("studentDiscountPercentage",
                appConfigService.get("student.discount.percentage", "10.00"));
        model.addAttribute("studentMinBillAmount",
                appConfigService.get("student.discount.min_bill_amount", "200.00"));
        return "admin/settings";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("dto") AppSettingsDto dto,
                       BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "admin/settings";
        }
        appConfigService.set("cafe.name",         dto.getCafeName().trim());
        appConfigService.set("cafe.address",       dto.getCafeAddress() != null ? dto.getCafeAddress().trim() : "");
        appConfigService.set("gst.rate",           dto.getGstRate().toPlainString());
        appConfigService.set("gst.inclusive",      String.valueOf(dto.isGstInclusive()));
        appConfigService.set("loyalty.earn_rate",  dto.getLoyaltyEarnRate().toPlainString());
        appConfigService.set("app.base-url",       dto.getAppBaseUrl() != null ? dto.getAppBaseUrl().trim() : "");
        return "redirect:/admin/settings?saved";
    }

    private static final Set<String> HTMX_EDITABLE_KEYS = Set.of(
            "table.idle.timeout.minutes",
            "app.base-url",
            "student.discount.percentage",
            "student.discount.min_bill_amount"
    );

    @PostMapping("/key")
    public String saveKey(@RequestParam String key,
                          @RequestParam String value,
                          Model model) {
        if (!HTMX_EDITABLE_KEYS.contains(key)) {
            model.addAttribute("error", "Unknown setting key");
            return "admin/fragments/save-status :: save-status";
        }
        appConfigService.set(key, value.trim());
        return "admin/fragments/save-status :: save-status";
    }

    private AppSettingsDto loadDto() {
        AppSettingsDto dto = new AppSettingsDto();
        dto.setCafeName(appConfigService.get("cafe.name", "Bullet Beats Café"));
        dto.setCafeAddress(appConfigService.get("cafe.address", ""));
        dto.setGstRate(appConfigService.getDecimal("gst.rate", new BigDecimal("18.00")));
        dto.setGstInclusive(appConfigService.getBoolean("gst.inclusive", false));
        dto.setLoyaltyEarnRate(appConfigService.getDecimal("loyalty.earn_rate", new BigDecimal("10.00")));
        dto.setAppBaseUrl(appConfigService.get("app.base-url", ""));
        dto.setIdleTimeoutMinutes(appConfigService.getInt("table.idle.timeout.minutes", 10));
        return dto;
    }
}
