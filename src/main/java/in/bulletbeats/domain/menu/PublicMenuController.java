package in.bulletbeats.domain.menu;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.menu.dto.CategoryNode;
import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/public/menu")
@RequiredArgsConstructor
public class PublicMenuController {

    private final MenuService menuService;
    private final CategoryService categoryService;
    private final AppConfigService appConfigService;

    @GetMapping
    public String menu(@RequestParam(required = false) Long categoryId, Model model) {
        model.addAttribute("categories", categoryService.getAllActive());
        applyItemsAndTree(categoryId, model);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("cafeName", appConfigService.get("cafe.name", "Bullet Beats Café"));
        return "public/menu";
    }

    @GetMapping("/items")
    public String items(@RequestParam(required = false) Long categoryId, Model model) {
        applyItemsAndTree(categoryId, model);
        return "public/menu :: items-list";
    }

    private void applyItemsAndTree(Long categoryId, Model model) {
        if (categoryId == null) {
            model.addAttribute("categoryTree", categoryService.getActiveCategoryTree());
            model.addAttribute("items", menuService.getAllItems());
            return;
        }
        Category selected = categoryService.getById(categoryId);
        if (selected.getParent() != null) {
            model.addAttribute("items", menuService.getItemsByCategory(categoryId));
            return;
        }
        List<Category> subs = categoryService.getActiveSubcategories(categoryId);
        if (subs.isEmpty()) {
            model.addAttribute("items", menuService.getItemsByCategory(categoryId));
            return;
        }
        List<MenuItem> combined = new ArrayList<>(menuService.getItemsByCategory(categoryId));
        for (Category sub : subs) {
            combined.addAll(menuService.getItemsByCategory(sub.getId()));
        }
        model.addAttribute("categoryTree", List.of(new CategoryNode(selected, subs)));
        model.addAttribute("items", combined);
    }
}
