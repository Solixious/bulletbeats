package in.bulletbeats.domain.menu;

import in.bulletbeats.domain.menu.dto.CategoryDto;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.repository.CategoryRepository;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.shared.exception.CategoryInUseException;
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
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final MenuService menuService;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public String list(Model model) {
        List<Category> topLevel = categoryService.getTopLevelCategories();
        Map<Long, List<Category>> subcategoriesByParent = new HashMap<>();
        Map<Long, Integer> itemCounts = new HashMap<>();
        for (Category c : topLevel) {
            itemCounts.put(c.getId(), menuService.getItemsByCategory(c.getId()).size());
            List<Category> subs = categoryService.getAllSubcategories(c.getId());
            subcategoriesByParent.put(c.getId(), subs);
            for (Category sub : subs) {
                itemCounts.put(sub.getId(), menuService.getItemsByCategory(sub.getId()).size());
            }
        }
        model.addAttribute("categories", topLevel);
        model.addAttribute("subcategoriesByParent", subcategoriesByParent);
        model.addAttribute("itemCounts", itemCounts);
        return "menu/categories/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long parentId, Model model) {
        CategoryDto dto = new CategoryDto();
        dto.setParentId(parentId);
        model.addAttribute("dto", dto);
        model.addAttribute("parentOptions", categoryService.getAllActive());
        model.addAttribute("mode", "create");
        return "menu/categories/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") CategoryDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("parentOptions", categoryService.getAllActive());
            model.addAttribute("mode", "create");
            return "menu/categories/form";
        }
        try {
            categoryService.create(dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            model.addAttribute("parentOptions", categoryService.getAllActive());
            model.addAttribute("mode", "create");
            return "menu/categories/form";
        }
        return "redirect:/admin/categories?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getById(id);
        CategoryDto dto = new CategoryDto();
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        List<Category> parentOptions = categoryService.getAllActive().stream()
                .filter(c -> !c.getId().equals(id))
                .toList();
        model.addAttribute("dto", dto);
        model.addAttribute("category", category);
        model.addAttribute("parentOptions", parentOptions);
        model.addAttribute("hasChildren", categoryRepository.existsByParentId(id));
        model.addAttribute("mode", "edit");
        return "menu/categories/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") CategoryDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("category", categoryService.getById(id));
            model.addAttribute("parentOptions", categoryService.getAllActive().stream()
                    .filter(c -> !c.getId().equals(id)).toList());
            model.addAttribute("hasChildren", categoryRepository.existsByParentId(id));
            model.addAttribute("mode", "edit");
            return "menu/categories/form";
        }
        try {
            categoryService.update(id, dto);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            model.addAttribute("category", categoryService.getById(id));
            model.addAttribute("parentOptions", categoryService.getAllActive().stream()
                    .filter(c -> !c.getId().equals(id)).toList());
            model.addAttribute("hasChildren", categoryRepository.existsByParentId(id));
            model.addAttribute("mode", "edit");
            return "menu/categories/form";
        }
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

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoryService.deleteSubcategory(id);
            return "redirect:/admin/categories?deleted";
        } catch (CategoryInUseException | IllegalArgumentException e) {
            ra.addFlashAttribute("deleteError", e.getMessage());
            return "redirect:/admin/categories";
        }
    }
}
