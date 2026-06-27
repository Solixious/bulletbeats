package in.bulletbeats.domain.user.controller;

import in.bulletbeats.domain.shared.enums.Role;
import in.bulletbeats.domain.user.dto.CreateUserDto;
import in.bulletbeats.domain.user.dto.UpdateUserDto;
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

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private Long currentUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    @GetMapping
    public String list(Model model, Authentication auth) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("currentUserId", currentUserId(auth));
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dto", new CreateUserDto());
        model.addAttribute("roles", Role.values());
        model.addAttribute("mode", "create");
        return "admin/users/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("dto") CreateUserDto dto,
                         BindingResult result,
                         Model model,
                         Authentication auth) {
        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("mode", "create");
            return "admin/users/form";
        }
        userService.createUser(dto, currentUserId(auth));
        return "redirect:/admin/users?created";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);
        UpdateUserDto dto = new UpdateUserDto();
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());
        model.addAttribute("user", user);
        model.addAttribute("dto", dto);
        model.addAttribute("roles", Role.values());
        model.addAttribute("mode", "edit");
        return "admin/users/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("dto") UpdateUserDto dto,
                         BindingResult result,
                         Model model,
                         Authentication auth) {
        if (id.equals(currentUserId(auth)) && !dto.isActive()) {
            result.rejectValue("active", "self.deactivate",
                    "You cannot deactivate your own account.");
        }
        if (result.hasErrors()) {
            model.addAttribute("user", userService.getUserById(id));
            model.addAttribute("roles", Role.values());
            model.addAttribute("mode", "edit");
            return "admin/users/form";
        }
        userService.updateUser(id, dto);
        return "redirect:/admin/users?updated";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            return "redirect:/admin/users/" + id + "/edit?passwordError";
        }
        userService.changePassword(id, newPassword);
        return "redirect:/admin/users?passwordReset";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id,
                               Model model,
                               Authentication auth,
                               HttpServletRequest request) {
        // Silent guard — cannot deactivate self
        if (id.equals(currentUserId(auth))) {
            return "redirect:/admin/users";
        }
        User user = userService.getUserById(id);
        UpdateUserDto dto = new UpdateUserDto();
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setActive(!user.isActive());
        userService.updateUser(id, dto);

        User updated = userService.getUserById(id);
        model.addAttribute("user", updated);
        model.addAttribute("currentUserId", currentUserId(auth));

        if ("true".equals(request.getHeader("HX-Request"))) {
            return "admin/users/fragments/user-row :: user-row";
        }
        return "redirect:/admin/users";
    }
}
