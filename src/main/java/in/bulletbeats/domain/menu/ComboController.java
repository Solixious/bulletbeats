package in.bulletbeats.domain.menu;

import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.menu.dto.ComboIngredientDto;
import in.bulletbeats.domain.menu.dto.CreateComboDto;
import in.bulletbeats.domain.menu.dto.UpdateComboDto;
import in.bulletbeats.domain.menu.entity.Combo;
import in.bulletbeats.domain.menu.service.ComboService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/combos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;
    private final InventoryService inventoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("combos", comboService.getAll());
        return "menu/combos/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        CreateComboDto dto = new CreateComboDto();
        dto.setIngredients(new ArrayList<>(List.of(new ComboIngredientDto())));
        model.addAttribute("dto", dto);
        model.addAttribute("groceryItems", inventoryService.getAllItems());
        model.addAttribute("mode", "create");
        return "menu/combos/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") CreateComboDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("groceryItems", inventoryService.getAllItems());
            model.addAttribute("mode", "create");
            return "menu/combos/form";
        }
        comboService.create(dto);
        return "redirect:/admin/combos?created";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("combo", comboService.getById(id));
        return "menu/combos/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Combo combo = comboService.getById(id);
        UpdateComboDto dto = new UpdateComboDto();
        dto.setName(combo.getName());
        dto.setDescription(combo.getDescription());
        dto.setIngredients(combo.getIngredients().stream()
                .map(ing -> {
                    ComboIngredientDto d = new ComboIngredientDto();
                    d.setGroceryItemId(ing.getGroceryItem().getId());
                    d.setQuantityRequired(ing.getQuantityRequired());
                    return d;
                })
                .toList());
        model.addAttribute("dto", dto);
        model.addAttribute("combo", combo);
        model.addAttribute("groceryItems", inventoryService.getAllItems());
        model.addAttribute("mode", "edit");
        return "menu/combos/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") UpdateComboDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("combo", comboService.getById(id));
            model.addAttribute("groceryItems", inventoryService.getAllItems());
            model.addAttribute("mode", "edit");
            return "menu/combos/form";
        }
        comboService.update(id, dto);
        return "redirect:/admin/combos/" + id + "?updated";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        comboService.deactivate(id);
        return "redirect:/admin/combos?deactivated";
    }

    @GetMapping("/ingredient-row")
    public String ingredientRow(@RequestParam int index, Model model) {
        model.addAttribute("groceryItems", inventoryService.getAllItems());
        model.addAttribute("index", index);
        return "menu/combos/fragments/ingredient-row :: ingredient-row";
    }
}
