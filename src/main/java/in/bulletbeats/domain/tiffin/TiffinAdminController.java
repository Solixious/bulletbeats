package in.bulletbeats.domain.tiffin;

import in.bulletbeats.domain.tiffin.TiffinMealType;
import in.bulletbeats.domain.tiffin.dto.TiffinPricingDto;
import in.bulletbeats.domain.tiffin.service.TiffinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequestMapping("/admin/tiffin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TiffinAdminController {

    private final TiffinService tiffinService;

    @GetMapping("/pricing")
    public String pricingPage(Model model) {
        Map<String, BigDecimal> prices = tiffinService.getPricingMap();
        TiffinPricingDto dto = new TiffinPricingDto();
        dto.setBreakfastPrice(prices.getOrDefault(TiffinMealType.BREAKFAST.name(), BigDecimal.ZERO));
        dto.setLunchPrice(prices.getOrDefault(TiffinMealType.LUNCH.name(), BigDecimal.ZERO));
        dto.setDinnerPrice(prices.getOrDefault(TiffinMealType.DINNER.name(), BigDecimal.ZERO));
        model.addAttribute("dto", dto);
        return "admin/tiffin-pricing";
    }

    @PostMapping("/pricing")
    public String savePricing(@Valid @ModelAttribute("dto") TiffinPricingDto dto,
                              BindingResult result,
                              RedirectAttributes ra,
                              Model model) {
        if (result.hasErrors()) return "admin/tiffin-pricing";
        tiffinService.updatePricing(dto);
        ra.addFlashAttribute("success", "Tiffin prices updated");
        return "redirect:/admin/tiffin/pricing";
    }
}
