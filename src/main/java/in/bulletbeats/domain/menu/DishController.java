package in.bulletbeats.domain.menu;

import in.bulletbeats.domain.menu.dto.CreateDishDto;
import in.bulletbeats.domain.menu.dto.DishIngredientDto;
import in.bulletbeats.domain.menu.dto.UpdateDishDto;
import in.bulletbeats.domain.menu.entity.Dish;
import in.bulletbeats.domain.menu.service.DishService;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.shared.exception.DishInUseException;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/admin/dishes")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;
    private final InventoryService inventoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("dishes", dishService.getAll());
        model.addAttribute("inactiveDishes", dishService.getInactive());
        return "menu/dishes/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        CreateDishDto dto = new CreateDishDto();
        dto.setIngredients(new ArrayList<>(List.of(new DishIngredientDto())));
        model.addAttribute("dto", dto);
        model.addAttribute("groceryItems", inventoryService.getAllItems());
        model.addAttribute("mode", "create");
        return "menu/dishes/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") CreateDishDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("groceryItems", inventoryService.getAllItems());
            model.addAttribute("mode", "create");
            return "menu/dishes/form";
        }
        dishService.create(dto);
        return "redirect:/admin/dishes?created";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("dish", dishService.getById(id));
        return "menu/dishes/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Dish dish = dishService.getById(id);
        UpdateDishDto dto = new UpdateDishDto();
        dto.setName(dish.getName());
        dto.setDescription(dish.getDescription());
        dto.setRecipeNotes(dish.getRecipeNotes());
        dto.setPrepTimeMinutes(dish.getPrepTimeMinutes());
        dto.setIngredients(dish.getIngredients().stream()
                .map(ing -> {
                    DishIngredientDto d = new DishIngredientDto();
                    d.setGroceryItemId(ing.getGroceryItem().getId());
                    d.setQuantityRequired(ing.getQuantityRequired());
                    return d;
                })
                .toList());
        model.addAttribute("dto", dto);
        model.addAttribute("dish", dish);
        model.addAttribute("groceryItems", inventoryService.getAllItems());
        model.addAttribute("mode", "edit");
        return "menu/dishes/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") UpdateDishDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("dish", dishService.getById(id));
            model.addAttribute("groceryItems", inventoryService.getAllItems());
            model.addAttribute("mode", "edit");
            return "menu/dishes/form";
        }
        dishService.update(id, dto);
        return "redirect:/admin/dishes/" + id + "?updated";
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id) {
        dishService.reactivate(id);
        return "redirect:/admin/dishes?reactivated";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        try {
            dishService.deactivate(id);
        } catch (DishInUseException e) {
            return "redirect:/admin/dishes?inUse";
        }
        return "redirect:/admin/dishes?deactivated";
    }

    @GetMapping("/ingredient-row")
    public String ingredientRow(@RequestParam int index, Model model) {
        model.addAttribute("groceryItems", inventoryService.getAllItems());
        model.addAttribute("index", index);
        return "menu/dishes/fragments/ingredient-row :: ingredient-row";
    }

    @GetMapping("/{id}/recipe-notes/edit-form")
    public String recipeNotesEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("dish", dishService.getById(id));
        return "menu/dishes/fragments/recipe-notes-edit-form :: recipe-notes-edit-form";
    }

    @GetMapping("/{id}/recipe-notes")
    public String recipeNotesFragment(@PathVariable Long id, Model model) {
        model.addAttribute("dish", dishService.getById(id));
        return "menu/dishes/fragments/recipe-notes :: recipe-notes";
    }

    @PatchMapping("/{id}/recipe-notes")
    public String updateRecipeNotes(@PathVariable Long id,
                                    @RequestParam(required = false) String recipeNotes,
                                    HttpServletRequest request, Model model) {
        Dish dish = dishService.updateRecipeNotes(id, recipeNotes);
        model.addAttribute("dish", dish);
        String htmxHeader = request.getHeader("HX-Request");
        if (htmxHeader != null) {
            return "menu/dishes/fragments/recipe-notes :: recipe-notes";
        }
        return "redirect:/admin/dishes/" + id + "?recipeUpdated";
    }
}
