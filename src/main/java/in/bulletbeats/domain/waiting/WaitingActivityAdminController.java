package in.bulletbeats.domain.waiting;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.waiting.dto.WaitingActivityDto;
import in.bulletbeats.domain.waiting.entity.WaitingActivityCategory;
import in.bulletbeats.domain.waiting.service.WaitingActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/waiting-activities")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class WaitingActivityAdminController {

    private final WaitingActivityService waitingActivityService;
    private final AppConfigService appConfigService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("items", waitingActivityService.getAll());
        model.addAttribute("categories", WaitingActivityCategory.values());
        model.addAttribute("waitingEnabled", appConfigService.getBoolean("waiting.enabled", true));
        model.addAttribute("teaserMessage", waitingActivityService.getTeaserMessage());
        model.addAttribute("helpMessage", waitingActivityService.getHelpMessage());
        return "admin/waiting-activities";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute WaitingActivityDto dto, RedirectAttributes ra) {
        try {
            waitingActivityService.create(dto);
            return "redirect:/admin/waiting-activities?created";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("itemError", e.getMessage());
            return "redirect:/admin/waiting-activities";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        waitingActivityService.toggleActive(id);
        return "redirect:/admin/waiting-activities";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        waitingActivityService.delete(id);
        return "redirect:/admin/waiting-activities?deleted";
    }

    @PostMapping("/settings")
    public String saveSettings(@RequestParam(required = false) String waitingEnabled,
                               @RequestParam String teaserMessage,
                               @RequestParam String helpMessage) {
        appConfigService.set("waiting.enabled", String.valueOf(waitingEnabled != null));
        appConfigService.set("waiting.teaser_message", teaserMessage.trim());
        appConfigService.set("waiting.help_message", helpMessage.trim());
        return "redirect:/admin/waiting-activities?saved";
    }
}
