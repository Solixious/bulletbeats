package in.bulletbeats.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/error")
public class AppErrorController implements ErrorController {

    @RequestMapping
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            return switch (statusCode) {
                case 404 -> "error/404";
                case 403 -> "error/403";
                default -> {
                    model.addAttribute("errorRef",
                            UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    yield "error/500";
                }
            };
        }
        return "error/500";
    }
}
