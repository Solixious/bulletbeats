package in.bulletbeats.domain.menu;

import in.bulletbeats.domain.menu.dto.AvailabilityOverrideDto;
import in.bulletbeats.domain.menu.dto.CreateMenuItemDto;
import in.bulletbeats.domain.menu.dto.UpdateMenuItemDto;
import in.bulletbeats.domain.menu.entity.MenuItem;
import in.bulletbeats.domain.menu.service.CategoryService;
import in.bulletbeats.domain.menu.service.ComboService;
import in.bulletbeats.domain.menu.service.DishService;
import in.bulletbeats.domain.menu.service.MenuService;
import in.bulletbeats.domain.user.entity.User;
import in.bulletbeats.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/menu")
@RequiredArgsConstructor
public class MenuAdminController {

    private final MenuService menuService;
    private final CategoryService categoryService;
    private final DishService dishService;
    private final ComboService comboService;
    private final UserService userService;

    private Long currentUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId,
                       Model model, HttpServletRequest request) {
        List<MenuItem> items = categoryId != null
                ? menuService.getItemsByCategoryForAdmin(categoryId)
                : menuService.getAllItemsForAdmin();
        model.addAttribute("items", items);
        model.addAttribute("categories", categoryService.getAllActive());
        model.addAttribute("selectedCategoryId", categoryId);

        if ("true".equals(request.getHeader("HX-Request"))) {
            return "menu/admin/list :: menu-table";
        }
        return "menu/admin/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("dto", new CreateMenuItemDto());
        model.addAttribute("categories", categoryService.getAllActive());
        model.addAttribute("dishes", dishService.getAll());
        model.addAttribute("combos", comboService.getAll());
        model.addAttribute("mode", "create");
        return "menu/admin/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String createItem(@Valid @ModelAttribute("dto") CreateMenuItemDto dto,
                             BindingResult result,
                             @RequestParam(value = "image", required = false) MultipartFile image,
                             Authentication auth, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.getAllActive());
            model.addAttribute("dishes", dishService.getAll());
            model.addAttribute("combos", comboService.getAll());
            model.addAttribute("mode", "create");
            return "menu/admin/form";
        }
        menuService.createItem(dto, image, currentUserId(auth));
        return "redirect:/admin/menu?created";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("item", menuService.getItemById(id));
        model.addAttribute("ingredients", menuService.getIngredients(id));
        model.addAttribute("priceHistory", menuService.getPriceHistory(id));
        model.addAttribute("availabilityLog", menuService.getAvailabilityLog(id));
        model.addAttribute("usernameMap", userService.getUsernameMap());
        return "menu/admin/detail";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        MenuItem item = menuService.getItemById(id);
        UpdateMenuItemDto dto = new UpdateMenuItemDto();
        dto.setName(item.getName());
        dto.setCategoryId(item.getCategory().getId());
        dto.setDishId(item.getDish() != null ? item.getDish().getId() : null);
        dto.setComboId(item.getCombo() != null ? item.getCombo().getId() : null);
        dto.setPrice(item.getPrice());
        dto.setDisplayOrder(item.getDisplayOrder());
        model.addAttribute("dto", dto);
        model.addAttribute("item", item);
        model.addAttribute("categories", categoryService.getAllActive());
        model.addAttribute("dishes", dishService.getAll());
        model.addAttribute("combos", comboService.getAll());
        model.addAttribute("mode", "edit");
        return "menu/admin/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateItem(@PathVariable Long id,
                             @Valid @ModelAttribute("dto") UpdateMenuItemDto dto,
                             BindingResult result,
                             @RequestParam(value = "image", required = false) MultipartFile image,
                             Authentication auth, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("item", menuService.getItemById(id));
            model.addAttribute("categories", categoryService.getAllActive());
            model.addAttribute("dishes", dishService.getAll());
            model.addAttribute("combos", comboService.getAll());
            model.addAttribute("mode", "edit");
            return "menu/admin/form";
        }
        menuService.updateItem(id, dto, image, currentUserId(auth));
        return "redirect:/admin/menu/" + id + "?updated";
    }

    @PatchMapping("/{id}/price")
    @PreAuthorize("hasRole('ADMIN')")
    public String updatePrice(@PathVariable Long id,
                              @RequestParam BigDecimal price,
                              Authentication auth, Model model,
                              HttpServletRequest request) {
        if (!"true".equals(request.getHeader("HX-Request"))) {
            return "redirect:/admin/menu/" + id;
        }
        menuService.updatePrice(id, price, currentUserId(auth));
        model.addAttribute("item", menuService.getItemById(id));
        return "menu/admin/fragments/price-cell :: price-cell";
    }

    @GetMapping("/{id}/price-edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String priceEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", menuService.getItemById(id));
        return "menu/admin/fragments/price-cell :: price-edit";
    }

    @PatchMapping("/{id}/availability")
    public String updateAvailability(@PathVariable Long id,
                                     @RequestParam(required = false) Boolean override,
                                     @RequestParam(required = false) String reason,
                                     Authentication auth, Model model,
                                     HttpServletRequest request) {
        if (!"true".equals(request.getHeader("HX-Request"))) {
            return "redirect:/admin/menu/" + id;
        }
        menuService.updateAvailabilityOverride(id, override, reason, currentUserId(auth));
        model.addAttribute("item", menuService.getItemById(id));
        return "menu/admin/fragments/availability-cell :: availability-cell";
    }

    @GetMapping("/{id}/availability-edit")
    public String availabilityEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", menuService.getItemById(id));
        return "menu/admin/fragments/availability-cell :: availability-edit";
    }

    @GetMapping("/{id}/price-cell")
    @PreAuthorize("hasRole('ADMIN')")
    public String priceCell(@PathVariable Long id, Model model) {
        model.addAttribute("item", menuService.getItemById(id));
        return "menu/admin/fragments/price-cell :: price-cell";
    }

    @GetMapping("/{id}/availability-cell")
    public String availabilityCell(@PathVariable Long id, Model model) {
        model.addAttribute("item", menuService.getItemById(id));
        return "menu/admin/fragments/availability-cell :: availability-cell";
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String deactivate(@PathVariable Long id) {
        menuService.deactivate(id);
        return "redirect:/admin/menu?deactivated";
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String reactivate(@PathVariable Long id) {
        menuService.reactivate(id);
        return "redirect:/admin/menu?reactivated";
    }

    @GetMapping("/dish-selector")
    @PreAuthorize("hasRole('ADMIN')")
    public String dishSelector(Model model) {
        model.addAttribute("dishes", dishService.getAll());
        return "menu/admin/fragments/dish-selector :: dish-selector";
    }

    @GetMapping("/combo-selector")
    @PreAuthorize("hasRole('ADMIN')")
    public String comboSelector(Model model) {
        model.addAttribute("combos", comboService.getAll());
        return "menu/admin/fragments/combo-selector :: combo-selector";
    }
}
