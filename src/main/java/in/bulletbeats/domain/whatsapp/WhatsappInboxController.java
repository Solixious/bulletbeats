package in.bulletbeats.domain.whatsapp;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.whatsapp.entity.WhatsappMessage;
import in.bulletbeats.domain.whatsapp.service.WhatsappMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        populateThread(number, model, null);
        return "whatsapp/fragments/thread :: thread";
    }

    @PostMapping("/inbox/reply")
    public String reply(@RequestParam String toNumber,
                        @RequestParam String body,
                        Model model) {
        String error = null;
        try {
            messageService.sendReply(toNumber, body);
        } catch (Exception e) {
            error = e.getMessage();
        }
        populateThread(toNumber, model, error);
        return "whatsapp/fragments/thread :: thread";
    }

    private void populateThread(String number, Model model, String replyError) {
        List<WhatsappMessage> messages = messageService.getThread(number);
        Optional<Customer> customer = messageService.findCustomerByPhone(number);
        model.addAttribute("messages", messages);
        model.addAttribute("fromNumber", number);
        model.addAttribute("customer", customer.orElse(null));
        model.addAttribute("displayName",
                customer.map(Customer::getName)
                        .orElseGet(() -> WhatsappMessageService.formatPhone(number)));
        model.addAttribute("withinWindow", messageService.isWithin24HourWindow(number));
        model.addAttribute("replyError", replyError);
    }
}
