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

    /** Polled by the conversation list every 15 s to refresh names + unread counts. */
    @GetMapping("/inbox/conversations")
    public String conversationList(Model model) {
        model.addAttribute("conversations", messageService.getConversations());
        return "whatsapp/fragments/conversation-list :: conversation-list";
    }

    /** Loads (or reloads) the full thread panel for a given number. Marks messages as read. */
    @GetMapping("/inbox/thread")
    public String thread(@RequestParam String number, Model model) {
        messageService.markThreadAsRead(number);
        populateThread(number, model, null);
        return "whatsapp/fragments/thread :: thread";
    }

    /** Polled by the open thread every 8 s — returns only the messages area. Marks as read. */
    @GetMapping("/inbox/thread/messages")
    public String messagesOnly(@RequestParam String number, Model model) {
        messageService.markThreadAsRead(number);
        model.addAttribute("messages", messageService.getThread(number));
        return "whatsapp/fragments/thread :: messages-body";
    }

    @PostMapping("/inbox/reply")
    public String reply(@RequestParam String toNumber,
                        @RequestParam String body,
                        Model model) {
        String error = null;
        try {
            messageService.sendReply(toNumber, body);
            messageService.markThreadAsRead(toNumber);
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
