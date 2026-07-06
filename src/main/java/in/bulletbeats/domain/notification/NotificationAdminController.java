package in.bulletbeats.domain.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final NotificationService notificationService;

    @PostMapping("/test")
    public String sendTest(@RequestParam String phone,
                           @RequestParam String message,
                           @RequestParam(defaultValue = "WHATSAPP") NotificationChannel channel,
                           Model model) {
        try {
            notificationService.testSend(phone, message, channel);
            model.addAttribute("success", true);
            model.addAttribute("channel", channel.name());
            model.addAttribute("phone", phone);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "admin/fragments/notification-test-result :: result";
    }
}
