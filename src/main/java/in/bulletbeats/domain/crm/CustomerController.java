package in.bulletbeats.domain.crm;

import in.bulletbeats.domain.admin.AppConfigService;
import in.bulletbeats.domain.billing.service.BillingService;
import in.bulletbeats.domain.crm.dto.AddNoteDto;
import in.bulletbeats.domain.crm.service.CustomerService;
import in.bulletbeats.domain.crm.service.LoyaltyService;
import in.bulletbeats.domain.shared.enums.Role;
import in.bulletbeats.domain.user.entity.User;
import in.bulletbeats.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final LoyaltyService loyaltyService;
    private final UserService userService;
    private final AppConfigService appConfigService;
    private final BillingService billingService;

    private Long currentUserId(Authentication auth) {
        return ((User) auth.getPrincipal()).getId();
    }

    private boolean isManager(Authentication auth) {
        Role role = ((User) auth.getPrincipal()).getRole();
        return role == Role.MANAGER || role == Role.ADMIN;
    }

    @GetMapping
    public String list(@RequestParam(required = false, defaultValue = "") String q,
                       Model model, HttpServletRequest request) {
        model.addAttribute("customers", customerService.search(q));
        model.addAttribute("query", q);
        if ("true".equals(request.getHeader("HX-Request"))) {
            return "crm/fragments/customer-list :: customer-list";
        }
        return "crm/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication auth) {
        boolean manager = isManager(auth);
        model.addAttribute("customer", customerService.getById(id));
        model.addAttribute("notes", customerService.getNotesForCustomer(id));
        model.addAttribute("loyaltyPoints", loyaltyService.getPointsBalance(id));
        model.addAttribute("loyaltyTransactions",
                manager ? loyaltyService.getTransactionsForCustomer(id, PageRequest.of(0, 10)) : null);
        model.addAttribute("recentBills", billingService.getRecentBillsForCustomer(id));
        model.addAttribute("noteDto", new AddNoteDto());
        model.addAttribute("isManager", manager);
        model.addAttribute("usernameMap", userService.getUsernameMap());
        model.addAttribute("studentDiscountPercentage",
                appConfigService.get("student.discount.percentage", "10"));
        model.addAttribute("minBillAmount",
                appConfigService.getDecimal("student.discount.min_bill_amount", new BigDecimal("200.00")));
        return "crm/detail";
    }

    @PostMapping("/{id}/notes")
    public String addNote(@PathVariable Long id,
                          @Valid @ModelAttribute("noteDto") AddNoteDto dto,
                          BindingResult result,
                          Authentication auth, Model model,
                          HttpServletRequest request) {
        if (!result.hasErrors()) {
            customerService.addNote(id, dto.getNote(), currentUserId(auth));
        }
        if ("true".equals(request.getHeader("HX-Request"))) {
            model.addAttribute("notes", customerService.getNotesForCustomer(id));
            model.addAttribute("customerId", id);
            model.addAttribute("usernameMap", userService.getUsernameMap());
            return "crm/fragments/notes-list :: notes-list";
        }
        return "redirect:/customers/" + id + "#notes";
    }

    @PostMapping("/{id}/toggle-vip")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String toggleVip(@PathVariable Long id,
                            Authentication auth, Model model,
                            HttpServletRequest request) {
        customerService.toggleVip(id, currentUserId(auth));
        if ("true".equals(request.getHeader("HX-Request"))) {
            model.addAttribute("customer", customerService.getById(id));
            model.addAttribute("isManager", true);
            return "crm/fragments/vip-badge :: vip-badge";
        }
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/mark-student")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String markAsStudent(@PathVariable Long id,
                                Authentication auth, Model model) {
        customerService.markAsStudent(id, currentUserId(auth));
        model.addAttribute("customer", customerService.getById(id));
        model.addAttribute("isManager", true);
        return "crm/fragments/student-badge :: student-badge";
    }

    @PostMapping("/{id}/unmark-student")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String unmarkAsStudent(@PathVariable Long id,
                                  Authentication auth, Model model) {
        customerService.unmarkAsStudent(id, currentUserId(auth));
        model.addAttribute("customer", customerService.getById(id));
        model.addAttribute("isManager", true);
        return "crm/fragments/student-badge :: student-badge";
    }

    @GetMapping("/{id}/contact-edit")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String contactEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.getById(id));
        return "crm/fragments/contact-section :: contact-edit";
    }

    @GetMapping("/{id}/contact-view")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String contactView(@PathVariable Long id, Model model, Authentication auth) {
        model.addAttribute("customer", customerService.getById(id));
        model.addAttribute("isManager", isManager(auth));
        model.addAttribute("studentDiscountPercentage",
                appConfigService.get("student.discount.percentage", "10"));
        model.addAttribute("minBillAmount",
                appConfigService.getDecimal("student.discount.min_bill_amount", new BigDecimal("200.00")));
        return "crm/fragments/contact-section :: contact-view";
    }

    @PostMapping("/{id}/edit-contact")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String updateContact(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam String phone,
                                Authentication auth, Model model) {
        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            model.addAttribute("customer", customerService.getById(id));
            model.addAttribute("contactError", "Name and phone are required");
            return "crm/fragments/contact-section :: contact-edit";
        }
        try {
            customerService.updateContact(id, name, phone, currentUserId(auth));
        } catch (IllegalArgumentException e) {
            model.addAttribute("customer", customerService.getById(id));
            model.addAttribute("contactError", e.getMessage());
            return "crm/fragments/contact-section :: contact-edit";
        }
        model.addAttribute("customer", customerService.getById(id));
        model.addAttribute("isManager", isManager(auth));
        model.addAttribute("studentDiscountPercentage",
                appConfigService.get("student.discount.percentage", "10"));
        model.addAttribute("minBillAmount",
                appConfigService.getDecimal("student.discount.min_bill_amount", new BigDecimal("200.00")));
        return "crm/fragments/contact-section :: contact-view";
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("customers", customerService.search(q));
        model.addAttribute("query", q);
        return "crm/fragments/customer-list :: customer-list";
    }
}
