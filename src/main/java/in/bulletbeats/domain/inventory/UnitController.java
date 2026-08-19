package in.bulletbeats.domain.inventory;

import in.bulletbeats.domain.inventory.dto.UnitConversionDto;
import in.bulletbeats.domain.inventory.dto.UnitDto;
import in.bulletbeats.domain.inventory.service.UnitService;
import in.bulletbeats.domain.shared.exception.DuplicateUnitConversionException;
import in.bulletbeats.domain.shared.exception.ResourceNotFoundException;
import in.bulletbeats.domain.shared.exception.UnitAlreadyExistsException;
import in.bulletbeats.domain.shared.exception.UnitInUseException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory/units")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("units", unitService.getAllUnits());
        model.addAttribute("conversions", unitService.getAllConversions());
        return "inventory/units/list";
    }

    @PostMapping("/new")
    public String createUnit(@ModelAttribute UnitDto dto, RedirectAttributes ra) {
        try {
            unitService.createUnit(dto);
            return "redirect:/inventory/units?created";
        } catch (UnitAlreadyExistsException | IllegalArgumentException e) {
            ra.addFlashAttribute("unitError", e.getMessage());
            return "redirect:/inventory/units";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteUnit(@PathVariable Long id, RedirectAttributes ra) {
        try {
            unitService.deleteUnit(id);
            return "redirect:/inventory/units?unitDeleted";
        } catch (UnitInUseException e) {
            ra.addFlashAttribute("unitError", e.getMessage());
            return "redirect:/inventory/units";
        }
    }

    @PostMapping("/conversions/new")
    public String createConversion(@ModelAttribute UnitConversionDto dto, RedirectAttributes ra) {
        try {
            unitService.createConversion(dto);
            return "redirect:/inventory/units?conversionCreated";
        } catch (DuplicateUnitConversionException | IllegalArgumentException | ResourceNotFoundException e) {
            ra.addFlashAttribute("conversionError", e.getMessage());
            return "redirect:/inventory/units";
        }
    }

    @PostMapping("/conversions/{id}/delete")
    public String deleteConversion(@PathVariable Long id) {
        unitService.deleteConversion(id);
        return "redirect:/inventory/units?conversionDeleted";
    }
}
