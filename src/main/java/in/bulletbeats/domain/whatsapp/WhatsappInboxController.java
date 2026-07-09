package in.bulletbeats.domain.whatsapp;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.whatsapp.entity.WhatsappMessage;
import in.bulletbeats.domain.whatsapp.service.WhatsappMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/whatsapp")
@RequiredArgsConstructor
public class WhatsappInboxController {

    private final WhatsappMessageService messageService;

    @GetMapping("/inbox")
    public String inbox(Model model) {
        model.addAttribute("conversations", messageService.getConversations());
        return "whatsapp/inbox";
    }

    @GetMapping("/inbox/thread")
    public String thread(@RequestParam String number, Model model) {
        List<WhatsappMessage> messages = messageService.getThread(number);
        Optional<Customer> customer = messageService.findCustomerByPhone(number);

        model.addAttribute("messages", messages);
        model.addAttribute("fromNumber", number);
        model.addAttribute("customer", customer.orElse(null));
        model.addAttribute("displayName",
                customer.map(Customer::getName)
                        .orElseGet(() -> WhatsappMessageService.formatPhone(number)));
        return "whatsapp/fragments/thread :: thread";
    }
}
