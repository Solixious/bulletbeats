package in.bulletbeats.domain.inventory;

import in.bulletbeats.domain.inventory.dto.InventoryCategoryDto;
import in.bulletbeats.domain.inventory.repository.GroceryItemRepository;
import in.bulletbeats.domain.inventory.service.InventoryCategoryService;
import in.bulletbeats.domain.shared.exception.InventoryCategoryInUseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inventory/categories")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class InventoryCategoryController {

    private final InventoryCategoryService categoryService;
    private final GroceryItemRepository groceryItemRepository;

    @GetMapping
    public String list(Model model) {
        var categories = categoryService.getAll();
        Map<Long, Long> itemCounts = new HashMap<>();
        for (var c : categories) {
            itemCounts.put(c.getId(), groceryItemRepository.countByCategoryId(c.getId()));
        }
        model.addAttribute("categories", categories);
        model.addAttribute("itemCounts", itemCounts);
        return "inventory/categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new InventoryCategoryDto());
        model.addAttribute("mode", "create");
        return "inventory/categories/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") InventoryCategoryDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mode", "create");
            return "inventory/categories/form";
        }
        try {
            categoryService.create(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            model.addAttribute("mode", "create");
            return "inventory/categories/form";
        }
        return "redirect:/inventory/categories?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var category = categoryService.getById(id);
        InventoryCategoryDto dto = new InventoryCategoryDto();
        dto.setName(category.getName());
        dto.setDisplayOrder(category.getDisplayOrder());
        model.addAttribute("dto", dto);
        model.addAttribute("category", category);
        model.addAttribute("mode", "edit");
        return "inventory/categories/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") InventoryCategoryDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("category", categoryService.getById(id));
            model.addAttribute("mode", "edit");
            return "inventory/categories/form";
        }
        try {
            categoryService.update(id, dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            model.addAttribute("category", categoryService.getById(id));
            model.addAttribute("mode", "edit");
            return "inventory/categories/form";
        }
        return "redirect:/inventory/categories?updated";
    }

    @PostMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestParam List<Long> ids,
                                        HttpServletRequest request) {
        categoryService.reorder(ids);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/inventory/categories")
                .build();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.delete(id);
            return "redirect:/inventory/categories?deleted";
        } catch (InventoryCategoryInUseException e) {
            ra.addFlashAttribute("deleteError", e.getMessage());
            return "redirect:/inventory/categories";
        }
    }
}
