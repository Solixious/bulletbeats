package in.bulletbeats.domain.platform;

import in.bulletbeats.domain.platform.dto.OnlinePlatformDto;
import in.bulletbeats.domain.platform.service.OnlinePlatformService;
import in.bulletbeats.domain.shared.exception.DuplicatePlatformException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/platforms")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class OnlinePlatformAdminController {

    private final OnlinePlatformService onlinePlatformService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("platforms", onlinePlatformService.listAll());
        return "platform/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new OnlinePlatformDto());
        model.addAttribute("mode", "create");
        return "platform/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") OnlinePlatformDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("mode", "create");
            return "platform/form";
        }
        try {
            onlinePlatformService.create(dto);
        } catch (DuplicatePlatformException e) {
            result.rejectValue("name", "duplicate", e.getMessage());
            model.addAttribute("mode", "create");
            return "platform/form";
        }
        return "redirect:/admin/platforms?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var platform = onlinePlatformService.getById(id);
        OnlinePlatformDto dto = new OnlinePlatformDto();
        dto.setName(platform.getName());
        model.addAttribute("platform", platform);
        model.addAttribute("dto", dto);
        model.addAttribute("mode", "edit");
        return "platform/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") OnlinePlatformDto dto,
                         BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("platform", onlinePlatformService.getById(id));
            model.addAttribute("mode", "edit");
            return "platform/form";
        }
        try {
            onlinePlatformService.update(id, dto);
        } catch (DuplicatePlatformException e) {
            result.rejectValue("name", "duplicate", e.getMessage());
            model.addAttribute("platform", onlinePlatformService.getById(id));
            model.addAttribute("mode", "edit");
            return "platform/form";
        }
        return "redirect:/admin/platforms?updated";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id) {
        onlinePlatformService.activate(id);
        return "redirect:/admin/platforms?activated";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id) {
        onlinePlatformService.deactivate(id);
        return "redirect:/admin/platforms?deactivated";
    }
}
