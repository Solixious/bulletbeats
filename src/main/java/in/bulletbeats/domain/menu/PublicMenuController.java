package in.bulletbeats.domain.menu;

import in.bulletbeats.domain.admin.AppConfigService;
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
        List<Category> categories = categoryService.getAllActive();
        List<MenuItem> items = categoryId != null
                ? menuService.getItemsByCategory(categoryId)
                : menuService.getAllItems();

        model.addAttribute("categories", categories);
        model.addAttribute("items", items);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("cafeName", appConfigService.get("cafe.name", "Bullet Beats Café"));
        return "public/menu";
    }
}
