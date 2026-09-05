package in.bulletbeats.domain.inventory;

import in.bulletbeats.domain.inventory.dto.CreatePreparedItemDto;
import in.bulletbeats.domain.inventory.dto.PrepareBatchDto;
import in.bulletbeats.domain.inventory.dto.PreparedItemIngredientDto;
import in.bulletbeats.domain.inventory.dto.PreparedItemStockAdjustmentDto;
import in.bulletbeats.domain.inventory.dto.UpdatePreparedItemDto;
import in.bulletbeats.domain.inventory.entity.PreparedItem;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.inventory.service.PreparedItemService;
import in.bulletbeats.domain.inventory.service.UnitService;
import in.bulletbeats.domain.shared.enums.MovementType;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.shared.exception.PreparedItemInUseException;
import in.bulletbeats.domain.user.entity.User;
import in.bulletbeats.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/prepared-items")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
@RequiredArgsConstructor
public class PreparedItemController {

    private final PreparedItemService preparedItemService;
    private final InventoryService inventoryService;
    private final UnitService unitService;
    private final UserService userService;

    private Long currentUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", preparedItemService.getAll());
        return "inventory/prepared-items/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        CreatePreparedItemDto dto = new CreatePreparedItemDto();
        dto.setIngredients(new ArrayList<>(List.of(new PreparedItemIngredientDto())));
        model.addAttribute("dto", dto);
        model.addAttribute("groceryItems", inventoryService.getAllItems());
        model.addAttribute("preparedItems", preparedItemService.getAll());
        model.addAttribute("units", unitService.getAllUnits());
        model.addAttribute("mode", "create");
        return "inventory/prepared-items/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") CreatePreparedItemDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("groceryItems", inventoryService.getAllItems());
            model.addAttribute("preparedItems", preparedItemService.getAll());
            model.addAttribute("units", unitService.getAllUnits());
            model.addAttribute("mode", "create");
            return "inventory/prepared-items/form";
        }
        PreparedItem created = preparedItemService.create(dto);
        return "redirect:/admin/prepared-items/" + created.getId() + "?created";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        PreparedItem item = preparedItemService.getById(id);
        model.addAttribute("item", item);
        model.addAttribute("costPerBatch", preparedItemService.computeCostPerBatch(item));
        model.addAttribute("costPerUnit", preparedItemService.computeCostPerUnit(item));
        model.addAttribute("movementTypes", MovementType.values());
        model.addAttribute("prepareDto", new PrepareBatchDto());
        model.addAttribute("adjustDto", new PreparedItemStockAdjustmentDto());
        return "inventory/prepared-items/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PreparedItem item = preparedItemService.getById(id);
        UpdatePreparedItemDto dto = new UpdatePreparedItemDto();
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setPrepTimeMinutes(item.getPrepTimeMinutes());
        dto.setUnit(item.getUnit());
        dto.setMinorUnit(item.getMinorUnit());
        dto.setBatchYieldQuantity(item.getBatchYieldQuantity());
        dto.setMinThreshold(item.getMinThreshold());
        dto.setIngredients(item.getIngredients().stream()
                .map(ing -> {
                    PreparedItemIngredientDto d = new PreparedItemIngredientDto();
                    if (ing.getGroceryItem() != null) {
                        d.setGroceryItemId(ing.getGroceryItem().getId());
                    } else {
                        d.setPreparedItemId(ing.getIngredientPreparedItem().getId());
                    }
                    d.setQuantityRequired(ing.getQuantityRequired());
                    return d;
                })
                .toList());
        model.addAttribute("dto", dto);
        model.addAttribute("item", item);
        model.addAttribute("groceryItems", inventoryService.getAllItems());
        model.addAttribute("preparedItems", preparedItemsExcluding(id));
        model.addAttribute("units", unitService.getAllUnits());
        model.addAttribute("mode", "edit");
        return "inventory/prepared-items/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") UpdatePreparedItemDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("item", preparedItemService.getById(id));
            model.addAttribute("groceryItems", inventoryService.getAllItems());
            model.addAttribute("preparedItems", preparedItemsExcluding(id));
            model.addAttribute("units", unitService.getAllUnits());
            model.addAttribute("mode", "edit");
            return "inventory/prepared-items/form";
        }
        preparedItemService.update(id, dto);
        return "redirect:/admin/prepared-items/" + id + "?updated";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            preparedItemService.deleteItem(id);
            return "redirect:/admin/prepared-items?deleted";
        } catch (PreparedItemInUseException e) {
            ra.addFlashAttribute("deleteError", e.getMessage());
            return "redirect:/admin/prepared-items/" + id;
        }
    }

    @GetMapping("/ingredient-row")
    public String ingredientRow(@RequestParam int index,
                                @RequestParam(required = false) Long excludeId,
                                Model model) {
        model.addAttribute("groceryItems", inventoryService.getAllItems());
        model.addAttribute("preparedItems", preparedItemsExcluding(excludeId));
        model.addAttribute("index", index);
        return "inventory/prepared-items/fragments/ingredient-row :: ingredient-row";
    }

    /** Prepared items eligible to be picked as an ingredient — a prepared item can't consume itself. */
    private List<PreparedItem> preparedItemsExcluding(Long id) {
        if (id == null) {
            return preparedItemService.getAll();
        }
        return preparedItemService.getAll().stream()
                .filter(pi -> !pi.getId().equals(id))
                .toList();
    }

    @PostMapping("/{id}/prepare")
    public String prepareBatch(@PathVariable Long id,
                               @Valid @ModelAttribute("prepareDto") PrepareBatchDto dto,
                               BindingResult result,
                               Model model,
                               Authentication auth) {
        if (result.hasErrors()) {
            return "redirect:/admin/prepared-items/" + id;
        }
        try {
            preparedItemService.prepareBatch(id, dto, currentUserId(auth));
            return "redirect:/admin/prepared-items/" + id + "?prepared";
        } catch (InsufficientStockException e) {
            model.addAttribute("item", preparedItemService.getById(id));
            model.addAttribute("prepareError", String.join("; ", e.getDetails()));
            return "redirect:/admin/prepared-items/" + id;
        }
    }

    @PostMapping("/{id}/adjust")
    public String adjustStock(@PathVariable Long id,
                              @Valid @ModelAttribute("adjustDto") PreparedItemStockAdjustmentDto dto,
                              BindingResult result,
                              Authentication auth) {
        if (!result.hasErrors()) {
            try {
                preparedItemService.adjustStock(id, dto, currentUserId(auth));
            } catch (InsufficientStockException ignored) {
                // fall through to redirect; detail page just shows current stock
            }
        }
        return "redirect:/admin/prepared-items/" + id;
    }

    @GetMapping("/{id}/movements")
    public String movements(@PathVariable Long id,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        model.addAttribute("movements", preparedItemService.getMovementsForItem(
                id, PageRequest.of(page, 15, Sort.by("createdAt").descending())));
        model.addAttribute("itemId", id);
        model.addAttribute("usernameMap", userService.getUsernameMap());
        return "inventory/prepared-items/fragments/movement-history :: movement-history";
    }
}
