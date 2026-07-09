package in.bulletbeats.domain.whatsapp;

import in.bulletbeats.domain.whatsapp.service.WhatsappMessageService;
import in.bulletbeats.domain.whatsapp.service.WhatsappSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WhatsappWebhookController {

    private final WhatsappMessageService messageService;
    private final WhatsappSseService sseService;

    /**
     * Twilio calls this endpoint when a WhatsApp message is received.
     * Must be public (no auth) and CSRF-exempt (see SecurityConfig).
     * Returns an empty TwiML response to suppress any auto-reply.
     */
    @PostMapping(value = "/whatsapp", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> receive(
            @RequestParam(value = "From", required = false) String from,
            @RequestParam(value = "Body", required = false) String body,
            @RequestParam(value = "MessageSid", required = false) String messageSid) {

        try {
            if (from == null || from.isBlank()) {
                log.warn("WhatsApp webhook called without From field");
                return ResponseEntity.badRequest().body("Missing From");
            }
            messageService.processIncoming(from, body, messageSid);
            sseService.notifyNewMessage();
        } catch (Exception e) {
            // Log and return 200 so Twilio does not retry
            log.error("Error processing WhatsApp webhook from={}: {}", from, e.getMessage(), e);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>");
    }
}
