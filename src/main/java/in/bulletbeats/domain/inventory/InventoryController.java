package in.bulletbeats.domain.inventory;

import in.bulletbeats.domain.inventory.dto.CreateGroceryItemDto;
import in.bulletbeats.domain.inventory.dto.StockAdjustmentDto;
import in.bulletbeats.domain.inventory.dto.UpdateGroceryItemDto;
import in.bulletbeats.domain.inventory.entity.GroceryItem;
import in.bulletbeats.domain.inventory.service.InventoryService;
import in.bulletbeats.domain.inventory.service.SupplierService;
import in.bulletbeats.domain.shared.enums.MovementType;
import in.bulletbeats.domain.shared.exception.InsufficientStockException;
import in.bulletbeats.domain.user.entity.User;
import in.bulletbeats.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
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

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final UserService userService;

    private Long currentUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", inventoryService.getItemsWithLowStockFlag());
        model.addAttribute("onOrderItemIds", inventoryService.getOnOrderGroceryItemIds());
        model.addAttribute("lowStockCount", inventoryService.getLowStockCount());
        return "inventory/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("dto", new CreateGroceryItemDto());
        model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
        model.addAttribute("mode", "create");
        return "inventory/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String create(@Valid @ModelAttribute("dto") CreateGroceryItemDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
            model.addAttribute("mode", "create");
            return "inventory/form";
        }
        inventoryService.createItem(dto);
        return "redirect:/inventory?created";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("item", inventoryService.getItemById(id));
        model.addAttribute("movementTypes", MovementType.values());
        model.addAttribute("dto", new StockAdjustmentDto());
        return "inventory/detail";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        GroceryItem item = inventoryService.getItemById(id);
        UpdateGroceryItemDto dto = new UpdateGroceryItemDto();
        dto.setName(item.getName());
        dto.setUnit(item.getUnit());
        dto.setMinThreshold(item.getMinThreshold());
        dto.setReorderQuantity(item.getReorderQuantity());
        if (item.getDefaultSupplier() != null) dto.setSupplierId(item.getDefaultSupplier().getId());
        model.addAttribute("item", item);
        model.addAttribute("dto", dto);
        model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
        model.addAttribute("mode", "edit");
        return "inventory/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") UpdateGroceryItemDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("item", inventoryService.getItemById(id));
            model.addAttribute("suppliers", supplierService.getAllActiveSuppliers());
            model.addAttribute("mode", "edit");
            return "inventory/form";
        }
        inventoryService.updateItem(id, dto);
        return "redirect:/inventory/" + id + "?updated";
    }

    @GetMapping("/units/suggest")
    public String suggestUnits(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("units", inventoryService.suggestUnits(q));
        return "inventory/fragments/unit-suggestions :: suggestions";
    }

    @GetMapping("/{id}/adjust-form")
    public String adjustForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", inventoryService.getItemById(id));
        model.addAttribute("movementTypes", MovementType.values());
        return "inventory/fragments/adjust-form :: adjust-form";
    }

    @PostMapping("/{id}/adjust")
    public String adjustStock(@PathVariable Long id,
                              @Valid @ModelAttribute("dto") StockAdjustmentDto dto,
                              BindingResult result,
                              Model model,
                              Authentication auth,
                              HttpServletRequest request) {
        boolean isHtmx = "true".equals(request.getHeader("HX-Request"));
        String hxTarget = request.getHeader("HX-Target");
        boolean detailTarget = "stock-info".equals(hxTarget);

        if (result.hasErrors()) {
            if (isHtmx) {
                model.addAttribute("item", inventoryService.getItemById(id));
                model.addAttribute("movementTypes", MovementType.values());
                model.addAttribute("adjustError", "Please fill in all required fields.");
                return detailTarget
                        ? "inventory/fragments/stock-info :: stock-info"
                        : "inventory/fragments/adjust-form :: adjust-result";
            }
            return "redirect:/inventory/" + id;
        }

        try {
            GroceryItem updated = inventoryService.adjustStock(id, dto, currentUserId(auth));
            model.addAttribute("item", updated);
            model.addAttribute("movementTypes", MovementType.values());
            if (isHtmx) {
                if (!detailTarget) {
                    model.addAttribute("lowStockCount", inventoryService.getLowStockCount());
                    model.addAttribute("onOrderItemIds", inventoryService.getOnOrderGroceryItemIds());
                }
                return detailTarget
                        ? "inventory/fragments/stock-info :: stock-info"
                        : "inventory/fragments/adjust-form :: adjust-result";
            }
        } catch (InsufficientStockException e) {
            if (isHtmx) {
                model.addAttribute("item", inventoryService.getItemById(id));
                model.addAttribute("movementTypes", MovementType.values());
                model.addAttribute("adjustError", String.join("; ", e.getDetails()));
                return detailTarget
                        ? "inventory/fragments/stock-info :: stock-info"
                        : "inventory/fragments/adjust-form :: adjust-result";
            }
        }
        return "redirect:/inventory/" + id;
    }

    @GetMapping("/{id}/movements")
    public String movements(@PathVariable Long id,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        model.addAttribute("movements", inventoryService.getMovementsForItem(
                id, PageRequest.of(page, 15, Sort.by("createdAt").descending())));
        model.addAttribute("itemId", id);
        model.addAttribute("usernameMap", userService.getUsernameMap());
        return "inventory/fragments/movement-history :: movement-history";
    }
}
