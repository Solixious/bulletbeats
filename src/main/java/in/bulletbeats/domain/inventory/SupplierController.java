package in.bulletbeats.domain.inventory;

import in.bulletbeats.domain.inventory.dto.SupplierDto;
import in.bulletbeats.domain.inventory.service.SupplierService;
import in.bulletbeats.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inventory/suppliers")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
        model.addAttribute("inactiveSuppliers", supplierService.getAllInactiveSuppliers());
        return "inventory/suppliers/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new SupplierDto());
        model.addAttribute("mode", "create");
        return "inventory/suppliers/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") SupplierDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mode", "create");
            return "inventory/suppliers/form";
        }
        supplierService.create(dto);
        return "redirect:/inventory/suppliers?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var supplier = supplierService.getById(id);
        SupplierDto dto = new SupplierDto();
        dto.setName(supplier.getName());
        dto.setPhone(supplier.getPhone());
        model.addAttribute("supplier", supplier);
        model.addAttribute("dto", dto);
        model.addAttribute("mode", "edit");
        return "inventory/suppliers/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") SupplierDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("supplier", supplierService.getById(id));
            model.addAttribute("mode", "edit");
            return "inventory/suppliers/form";
        }
        supplierService.update(id, dto);
        return "redirect:/inventory/suppliers?updated";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        supplierService.deactivate(id);
        return "redirect:/inventory/suppliers?deactivated";
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id) {
        supplierService.reactivate(id);
        return "redirect:/inventory/suppliers?reactivated";
    }
}
