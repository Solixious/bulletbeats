package in.bulletbeats.domain.user.controller;

import in.bulletbeats.domain.shared.enums.Role;
import in.bulletbeats.domain.user.dto.CreateUserDto;
import in.bulletbeats.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/setup")
@RequiredArgsConstructor
public class SetupController {

    private final UserService userService;

    @GetMapping
    public String showSetup(Model model) {
        if (userService.isSetupCompleted()) {
            return "redirect:/login";
        }
        model.addAttribute("createUserDto", new CreateUserDto());
        return "auth/setup";
    }

    @PostMapping
    public String processSetup(@Valid @ModelAttribute("createUserDto") CreateUserDto dto,
                               BindingResult result) {
        if (result.hasErrors()) {
            return "auth/setup";
        }
        dto.setRole(Role.ADMIN);
        userService.createUser(dto, null);
        userService.markSetupCompleted();
        return "redirect:/login?setup";
    }
}
