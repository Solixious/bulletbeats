package in.bulletbeats.domain.offers.controller;

import in.bulletbeats.domain.offers.dto.CustomerCohortDto;
import in.bulletbeats.domain.offers.entity.CustomerCohort;
import in.bulletbeats.domain.offers.entity.enums.CohortRuleField;
import in.bulletbeats.domain.offers.entity.enums.CohortRuleOperator;
import in.bulletbeats.domain.offers.service.CustomerCohortAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/cohorts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CustomerCohortAdminController {

    private final CustomerCohortAdminService cohortAdminService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("cohorts", cohortAdminService.getAll());
        return "admin/cohorts/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new CustomerCohortDto());
        model.addAttribute("mode", "create");
        addFormOptions(model);
        return "admin/cohorts/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") CustomerCohortDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mode", "create");
            addFormOptions(model);
            return "admin/cohorts/form";
        }
        cohortAdminService.create(dto);
        return "redirect:/admin/cohorts?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CustomerCohort cohort = cohortAdminService.getById(id);
        CustomerCohortDto dto = new CustomerCohortDto();
        dto.setName(cohort.getName());
        dto.setDescription(cohort.getDescription());
        dto.setRules(cohort.getRules().stream().map(r -> {
            var ruleDto = new in.bulletbeats.domain.offers.dto.CohortRuleDto();
            ruleDto.setField(r.getField());
            ruleDto.setOperator(r.getOperator());
            ruleDto.setValue(r.getValue());
            ruleDto.setPeriodDays(r.getPeriodDays());
            return ruleDto;
        }).toList());
        model.addAttribute("dto", dto);
        model.addAttribute("cohort", cohort);
        model.addAttribute("mode", "edit");
        addFormOptions(model);
        return "admin/cohorts/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("dto") CustomerCohortDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("cohort", cohortAdminService.getById(id));
            model.addAttribute("mode", "edit");
            addFormOptions(model);
            return "admin/cohorts/form";
        }
        cohortAdminService.update(id, dto);
        return "redirect:/admin/cohorts?updated";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            cohortAdminService.delete(id);
            return "redirect:/admin/cohorts?deleted";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("deleteError", e.getMessage());
            return "redirect:/admin/cohorts";
        }
    }

    private void addFormOptions(Model model) {
        model.addAttribute("fields", CohortRuleField.values());
        model.addAttribute("operators", CohortRuleOperator.values());
    }
}
