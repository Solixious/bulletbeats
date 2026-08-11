package in.bulletbeats.domain.offers.controller;

import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.offers.dto.OfferCodeDto;
import in.bulletbeats.domain.offers.dto.OfferDto;
import in.bulletbeats.domain.offers.entity.Offer;
import in.bulletbeats.domain.offers.entity.enums.CodeUsageType;
import in.bulletbeats.domain.offers.entity.enums.OfferMechanism;
import in.bulletbeats.domain.offers.entity.enums.OfferTargetType;
import in.bulletbeats.domain.offers.service.CustomerCohortAdminService;
import in.bulletbeats.domain.offers.service.OfferAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/offers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class OfferAdminController {

    private final OfferAdminService offerAdminService;
    private final CustomerCohortAdminService cohortAdminService;
    private final MenuService menuService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("offers", offerAdminService.getAll());
        return "admin/offers/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new OfferDto());
        model.addAttribute("mode", "create");
        addFormOptions(model);
        return "admin/offers/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") OfferDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mode", "create");
            addFormOptions(model);
            return "admin/offers/form";
        }
        try {
            offerAdminService.create(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            model.addAttribute("mode", "create");
            addFormOptions(model);
            return "admin/offers/form";
        }
        return "redirect:/admin/offers?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Offer offer = offerAdminService.getById(id);
        model.addAttribute("dto", toDto(offer));
        model.addAttribute("offer", offer);
        model.addAttribute("mode", "edit");
        model.addAttribute("codes", offerAdminService.getCodes(id));
        model.addAttribute("codeDto", new OfferCodeDto());
        model.addAttribute("usageTypes", CodeUsageType.values());
        addFormOptions(model);
        return "admin/offers/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("dto") OfferDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("offer", offerAdminService.getById(id));
            model.addAttribute("mode", "edit");
            model.addAttribute("codes", offerAdminService.getCodes(id));
            model.addAttribute("codeDto", new OfferCodeDto());
            model.addAttribute("usageTypes", CodeUsageType.values());
            addFormOptions(model);
            return "admin/offers/form";
        }
        try {
            offerAdminService.update(id, dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            model.addAttribute("offer", offerAdminService.getById(id));
            model.addAttribute("mode", "edit");
            model.addAttribute("codes", offerAdminService.getCodes(id));
            model.addAttribute("codeDto", new OfferCodeDto());
            model.addAttribute("usageTypes", CodeUsageType.values());
            addFormOptions(model);
            return "admin/offers/form";
        }
        return "redirect:/admin/offers?updated";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id) {
        offerAdminService.toggleActive(id);
        return "redirect:/admin/offers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            offerAdminService.delete(id);
            return "redirect:/admin/offers?deleted";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("deleteError", e.getMessage());
            return "redirect:/admin/offers";
        }
    }

    @PostMapping("/{id}/codes")
    public String generateCode(@PathVariable Long id, @ModelAttribute OfferCodeDto codeDto,
                               RedirectAttributes ra) {
        try {
            offerAdminService.generateCode(id, codeDto);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("codeError", e.getMessage());
        }
        return "redirect:/admin/offers/" + id + "/edit";
    }

    @PostMapping("/{id}/codes/{codeId}/deactivate")
    public String deactivateCode(@PathVariable Long id, @PathVariable Long codeId) {
        offerAdminService.setCodeActive(codeId, false);
        return "redirect:/admin/offers/" + id + "/edit";
    }

    @PostMapping("/{id}/codes/{codeId}/reactivate")
    public String reactivateCode(@PathVariable Long id, @PathVariable Long codeId) {
        offerAdminService.setCodeActive(codeId, true);
        return "redirect:/admin/offers/" + id + "/edit";
    }

    @PostMapping("/{id}/codes/{codeId}/delete")
    public String deleteCode(@PathVariable Long id, @PathVariable Long codeId) {
        offerAdminService.deleteCode(codeId);
        return "redirect:/admin/offers/" + id + "/edit";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("mechanisms", OfferMechanism.values());
        model.addAttribute("targetTypes", OfferTargetType.values());
        model.addAttribute("cohorts", cohortAdminService.getAll());
        model.addAttribute("menuItems", menuService.getAllItems());
        model.addAttribute("categories", categoryService.getAllActive());
    }

    private OfferDto toDto(Offer offer) {
        OfferDto dto = new OfferDto();
        dto.setName(offer.getName());
        dto.setDescription(offer.getDescription());
        dto.setMechanism(offer.getMechanism());
        dto.setPercentageValue(offer.getPercentageValue());
        dto.setFixedValue(offer.getFixedValue());
        dto.setBuyQuantity(offer.getBuyQuantity());
        dto.setGetQuantity(offer.getGetQuantity());
        dto.setTargetType(offer.getTargetType());
        dto.setCohortId(offer.getCohort() != null ? offer.getCohort().getId() : null);
        dto.setCustomerPhone(offer.getCustomer() != null ? offer.getCustomer().getPhone() : null);
        dto.setMinSpend(offer.getMinSpend());
        dto.setStartsAt(offer.getStartsAt());
        dto.setEndsAt(offer.getEndsAt());
        dto.setRequiresCode(offer.isRequiresCode());
        dto.setMaxTotalUses(offer.getMaxTotalUses());
        dto.setMaxUsesPerCustomer(offer.getMaxUsesPerCustomer());
        dto.setActive(offer.isActive());
        dto.setMenuItemIds(offer.getItems().stream()
                .filter(i -> i.getMenuItem() != null)
                .map(i -> i.getMenuItem().getId())
                .toList());
        dto.setCategoryIds(offer.getItems().stream()
                .filter(i -> i.getCategory() != null)
                .map(i -> i.getCategory().getId())
                .toList());
        return dto;
    }
}
