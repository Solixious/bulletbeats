package in.bulletbeats.web;

import in.bulletbeats.config.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final LoginAttemptService loginAttemptService;

    @GetMapping("/login")
    public String login(Authentication authentication, HttpServletRequest request, Model model) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        String ip = loginAttemptService.getClientIp(request);
        model.addAttribute("remainingAttempts", loginAttemptService.getRemainingAttempts(ip));
        model.addAttribute("loginAttemptService", loginAttemptService);
        return "auth/login";
    }

    @GetMapping("/403")
    public String forbidden() {
        return "error/403";
    }
}
