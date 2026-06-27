package in.bulletbeats.domain.menu;

import in.bulletbeats.domain.menu.dto.CategoryDto;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final MenuService menuService;

    @GetMapping
    public String list(Model model) {
        List<Category> active = categoryService.getAllActive();
        List<Category> inactive = categoryService.getAllInactive();
        Map<Long, Integer> itemCounts = new HashMap<>();
        for (Category c : active) {
            itemCounts.put(c.getId(), menuService.getItemsByCategory(c.getId()).size());
        }
        for (Category c : inactive) {
            itemCounts.put(c.getId(), menuService.getItemsByCategory(c.getId()).size());
        }
        model.addAttribute("categories", active);
        model.addAttribute("inactiveCategories", inactive);
        model.addAttribute("itemCounts", itemCounts);
        return "menu/categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new CategoryDto());
        model.addAttribute("mode", "create");
        return "menu/categories/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") CategoryDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mode", "create");
            return "menu/categories/form";
        }
        categoryService.create(dto);
        return "redirect:/admin/categories?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getById(id);
        CategoryDto dto = new CategoryDto();
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setDisplayOrder(category.getDisplayOrder());
        model.addAttribute("dto", dto);
        model.addAttribute("category", category);
        model.addAttribute("mode", "edit");
        return "menu/categories/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") CategoryDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("category", categoryService.getById(id));
            model.addAttribute("mode", "edit");
            return "menu/categories/form";
        }
        categoryService.update(id, dto);
        return "redirect:/admin/categories?updated";
    }

    @PostMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestParam List<Long> ids,
                                        HttpServletRequest request) {
        categoryService.reorder(ids);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/admin/categories")
                .build();
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        categoryService.deactivate(id);
        return "redirect:/admin/categories?deactivated";
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id) {
        categoryService.reactivate(id);
        return "redirect:/admin/categories?reactivated";
    }
}
