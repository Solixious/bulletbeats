package in.bulletbeats.domain.tiffin;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.service.CustomerService;
import in.bulletbeats.domain.tiffin.dto.TiffinPauseDto;
import in.bulletbeats.domain.tiffin.dto.TiffinPaymentDto;
import in.bulletbeats.domain.tiffin.dto.TiffinPricingOverrideDto;
import in.bulletbeats.domain.tiffin.dto.TiffinSubscriptionDto;
import in.bulletbeats.domain.tiffin.entity.TiffinSubscription;
import in.bulletbeats.domain.tiffin.service.TiffinService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/tiffin")
@RequiredArgsConstructor
public class TiffinController {

    private final TiffinService tiffinService;
    private final CustomerService customerService;

    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "active") String filter,
                       Model model) {
        List<TiffinSubscription> subs = "all".equals(filter)
                ? tiffinService.getAll()
                : tiffinService.getActiveAndPaused();

        java.util.Map<Long, java.math.BigDecimal> monthlyPrices = new java.util.LinkedHashMap<>();
        subs.forEach(s -> monthlyPrices.put(s.getId(), tiffinService.calculateMonthlyPrice(s)));

        model.addAttribute("subscriptions", subs);
        model.addAttribute("monthlyPrices", monthlyPrices);
        model.addAttribute("paidThroughMap",
                tiffinService.getPaidThroughMap(subs.stream().map(TiffinSubscription::getId).toList()));
        model.addAttribute("filter", filter);
        return "tiffin/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new TiffinSubscriptionDto());
        model.addAttribute("editing", false);
        return "tiffin/form";
    }

    @GetMapping("/customer-search")
    public String customerSearch(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("q", q.trim());
        model.addAttribute("customers", q.isBlank() ? List.of() : customerService.search(q.trim()));
        return "tiffin/fragments/customer-search-results :: customer-search-results";
    }

    @PostMapping("/quick-create-customer")
    public String quickCreateCustomer(@RequestParam(defaultValue = "") String name,
                                      @RequestParam(defaultValue = "") String phone,
                                      Model model) {
        if (name.isBlank() || phone.isBlank()) {
            model.addAttribute("error", "Name and phone are required");
            return "tiffin/fragments/customer-search-results :: quick-create-error";
        }
        Customer customer = customerService.findOrCreateByPhone(phone.trim(), name.trim(), null);
        model.addAttribute("customer", customer);
        return "tiffin/fragments/customer-search-results :: quick-create-success";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("dto") TiffinSubscriptionDto dto,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (!dto.isHasBreakfast() && !dto.isHasLunch() && !dto.isHasDinner()) {
            result.rejectValue("hasBreakfast", "meals.required", "Select at least one meal");
        }
        if (!dto.isAllDays() && (dto.getSelectedDays() == null || dto.getSelectedDays().isEmpty())) {
            result.rejectValue("selectedDays", "days.required", "Select at least one delivery day");
        }
        if (result.hasErrors()) {
            model.addAttribute("editing", false);
            return "tiffin/form";
        }
        if (tiffinService.hasActiveSubscription(dto.getCustomerId())) {
            result.rejectValue("customerId", "subscription.exists",
                    "This customer already has an active tiffin subscription");
            model.addAttribute("editing", false);
            return "tiffin/form";
        }

        TiffinSubscription sub = tiffinService.subscribe(dto);
        ra.addFlashAttribute("success", "Tiffin subscription added");
        return "redirect:/tiffin/" + sub.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        TiffinSubscription sub = tiffinService.getById(id);
        model.addAttribute("subscription", sub);
        model.addAttribute("pauses", tiffinService.getPausesForSubscription(id));
        model.addAttribute("pauseDto", new TiffinPauseDto());
        model.addAttribute("monthlyPrice", tiffinService.calculateMonthlyPrice(sub));

        TiffinPricingOverrideDto pricingDto = new TiffinPricingOverrideDto();
        pricingDto.setCustomMonthlyPrice(sub.getCustomMonthlyPrice());
        pricingDto.setDeliveryCharge(sub.getDeliveryCharge());
        model.addAttribute("pricingDto", pricingDto);

        model.addAttribute("payments", tiffinService.getPaymentsForSubscription(id));
        model.addAttribute("paidThroughDate", tiffinService.getPaidThroughDate(id));
        model.addAttribute("totalPaid", tiffinService.getTotalPaid(id));
        TiffinPaymentDto paymentDto = new TiffinPaymentDto();
        paymentDto.setCoverageFrom(tiffinService.getNextCoverageStart(sub));
        paymentDto.setAmountPaid(tiffinService.calculateMonthlyPrice(sub));
        model.addAttribute("paymentDto", paymentDto);

        return "tiffin/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TiffinSubscription sub = tiffinService.getById(id);
        TiffinSubscriptionDto dto = new TiffinSubscriptionDto();
        dto.setCustomerId(sub.getCustomer().getId());
        dto.setCustomerName(sub.getCustomer().getName());
        dto.setHasBreakfast(sub.isHasBreakfast());
        dto.setHasLunch(sub.isHasLunch());
        dto.setHasDinner(sub.isHasDinner());
        dto.setAllDays(sub.isAllDays());
        if (!sub.isAllDays() && sub.getCustomDays() != null) {
            dto.setSelectedDays(List.of(sub.getCustomDays().split(",")));
        }
        dto.setStartDate(sub.getStartDate());
        model.addAttribute("dto", dto);
        model.addAttribute("subscriptionId", id);
        model.addAttribute("editing", true);
        return "tiffin/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") TiffinSubscriptionDto dto,
                         BindingResult result,
                         RedirectAttributes ra,
                         Model model) {
        if (!dto.isHasBreakfast() && !dto.isHasLunch() && !dto.isHasDinner()) {
            result.rejectValue("hasBreakfast", "meals.required", "Select at least one meal");
        }
        if (!dto.isAllDays() && (dto.getSelectedDays() == null || dto.getSelectedDays().isEmpty())) {
            result.rejectValue("selectedDays", "days.required", "Select at least one delivery day");
        }
        if (result.hasErrors()) {
            model.addAttribute("subscriptionId", id);
            model.addAttribute("editing", true);
            return "tiffin/form";
        }

        tiffinService.update(id, dto);
        ra.addFlashAttribute("success", "Subscription updated");
        return "redirect:/tiffin/" + id;
    }

    @PostMapping("/{id}/pause")
    public String pause(@PathVariable Long id,
                        @Valid @ModelAttribute("pauseDto") TiffinPauseDto dto,
                        BindingResult result,
                        RedirectAttributes ra,
                        HttpServletRequest request,
                        Model model) {
        if (result.hasErrors()) {
            if ("true".equals(request.getHeader("HX-Request"))) {
                model.addAttribute("subscription", tiffinService.getById(id));
                model.addAttribute("pauseDto", dto);
                return "tiffin/fragments/pause-form :: pause-form";
            }
            return "redirect:/tiffin/" + id;
        }
        tiffinService.pause(id, dto);
        ra.addFlashAttribute("success", "Subscription paused");
        return "redirect:/tiffin/" + id;
    }

    @PostMapping("/{id}/resume")
    public String resume(@PathVariable Long id, RedirectAttributes ra) {
        tiffinService.resume(id);
        ra.addFlashAttribute("success", "Subscription resumed");
        return "redirect:/tiffin/" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        tiffinService.cancel(id);
        ra.addFlashAttribute("success", "Subscription cancelled");
        return "redirect:/tiffin";
    }

    @PostMapping("/{id}/payments")
    public String recordPayment(@PathVariable Long id,
                                @Valid @ModelAttribute("paymentDto") TiffinPaymentDto dto,
                                BindingResult result,
                                RedirectAttributes ra,
                                Model model) {
        if (dto.getCoverageFrom() != null && dto.getCoverageUntil() != null
                && dto.getCoverageUntil().isBefore(dto.getCoverageFrom())) {
            result.rejectValue("coverageUntil", "coverage.invalid",
                    "Coverage end date can't be before the start date");
        }
        if (result.hasErrors()) {
            TiffinSubscription sub = tiffinService.getById(id);
            model.addAttribute("subscription", sub);
            model.addAttribute("pauses", tiffinService.getPausesForSubscription(id));
            model.addAttribute("pauseDto", new TiffinPauseDto());
            model.addAttribute("monthlyPrice", tiffinService.calculateMonthlyPrice(sub));
            TiffinPricingOverrideDto pricingDto = new TiffinPricingOverrideDto();
            pricingDto.setCustomMonthlyPrice(sub.getCustomMonthlyPrice());
            pricingDto.setDeliveryCharge(sub.getDeliveryCharge());
            model.addAttribute("pricingDto", pricingDto);
            model.addAttribute("payments", tiffinService.getPaymentsForSubscription(id));
            model.addAttribute("paidThroughDate", tiffinService.getPaidThroughDate(id));
            model.addAttribute("totalPaid", tiffinService.getTotalPaid(id));
            return "tiffin/detail";
        }

        tiffinService.recordPayment(id, dto);
        ra.addFlashAttribute("success", "Payment recorded");
        return "redirect:/tiffin/" + id;
    }

    @PostMapping("/{id}/pricing")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String updatePricing(@PathVariable Long id,
                                @Valid @ModelAttribute("pricingDto") TiffinPricingOverrideDto dto,
                                BindingResult result,
                                RedirectAttributes ra,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("subscription", tiffinService.getById(id));
            model.addAttribute("pauses", tiffinService.getPausesForSubscription(id));
            model.addAttribute("pauseDto", new TiffinPauseDto());
            model.addAttribute("monthlyPrice", tiffinService.calculateMonthlyPrice(tiffinService.getById(id)));
            return "tiffin/detail";
        }
        tiffinService.updatePricingOverride(id, dto);
        ra.addFlashAttribute("success", "Pricing updated");
        return "redirect:/tiffin/" + id;
    }
}
