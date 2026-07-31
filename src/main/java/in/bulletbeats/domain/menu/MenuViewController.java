package in.bulletbeats.domain.menu;

import in.bulletbeats.domain.menu.entity.Category;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/menu")
@RequiredArgsConstructor
public class MenuViewController {

    private final MenuService menuService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId,
                       Model model, HttpServletRequest request) {
        List<Category> categories = categoryService.getAllActive();
        List<MenuItem> items;

        if (categoryId == null) {
            items = menuService.getAllItemsTreeOrdered();
        } else {
            Category selected = categoryService.getById(categoryId);
            List<Category> subs = selected.getParent() == null
                    ? categoryService.getActiveSubcategories(categoryId)
                    : List.of();
            if (subs.isEmpty()) {
                items = menuService.getItemsByCategory(categoryId);
            } else {
                items = new ArrayList<>(menuService.getItemsByCategory(categoryId));
                for (Category sub : subs) {
                    items.addAll(menuService.getItemsByCategory(sub.getId()));
                }
            }
        }

        model.addAttribute("categories", categories);
        model.addAttribute("items", items);
        model.addAttribute("selectedCategoryId", categoryId);

        if ("true".equals(request.getHeader("HX-Request"))) {
            return "menu/view/list :: menu-table";
        }
        return "menu/view/list";
    }
}
