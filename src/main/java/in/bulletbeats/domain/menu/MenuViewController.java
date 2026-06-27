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
        List<MenuItem> items = categoryId != null
                ? menuService.getItemsByCategory(categoryId)
                : menuService.getAllItems();

        model.addAttribute("categories", categories);
        model.addAttribute("items", items);
        model.addAttribute("selectedCategoryId", categoryId);

        if ("true".equals(request.getHeader("HX-Request"))) {
            return "menu/view/list :: menu-table";
        }
        return "menu/view/list";
    }
}
