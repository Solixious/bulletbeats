package in.bulletbeats.domain.whatsapp.service;

import in.bulletbeats.domain.crm.entity.Customer;
import in.bulletbeats.domain.crm.repository.CustomerRepository;
import in.bulletbeats.domain.notification.NotificationChannel;
import in.bulletbeats.domain.notification.NotificationService;
import in.bulletbeats.domain.whatsapp.entity.MessageDirection;
import in.bulletbeats.domain.whatsapp.entity.WhatsappMessage;
import in.bulletbeats.domain.whatsapp.repository.WhatsappMessageRepository;
import in.bulletbeats.domain.whatsapp.repository.WhatsappMessageRepository.ConversationRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappMessageService {

    private final WhatsappMessageRepository messageRepository;
    private final CustomerRepository customerRepository;
    private final NotificationService notificationService;

    /**
     * Sends a free-form reply to the customer via WhatsApp and stores it as an OUTBOUND message.
     * Throws if Twilio is not configured or if the 24-hour service window has closed.
     */
    @Transactional
    public void sendReply(String toNumber, String body) {
        notificationService.testSend(toNumber, body, NotificationChannel.WHATSAPP);

        WhatsappMessage msg = WhatsappMessage.builder()
                .fromNumber(toNumber)
                .body(body)
                .direction(MessageDirection.OUTBOUND)
                .isRead(true)
                .receivedAt(LocalDateTime.now())
                .build();
        messageRepository.save(msg);
        log.info("WhatsApp reply sent to={}", toNumber);
    }

    @Transactional
    public void markThreadAsRead(String fromNumber) {
        messageRepository.markAllAsRead(fromNumber, MessageDirection.INBOUND);
    }

    /** Returns true if the customer sent a message within the last 24 hours. */
    @Transactional(readOnly = true)
    public boolean isWithin24HourWindow(String fromNumber) {
        return messageRepository
                .findTopByFromNumberAndDirectionOrderByReceivedAtDesc(fromNumber, MessageDirection.INBOUND)
                .map(msg -> msg.getReceivedAt().isAfter(LocalDateTime.now().minusHours(24)))
                .orElse(false);
    }

    @Transactional
    public void processIncoming(String from, String body, String twilioSid) {
        if (twilioSid != null && !twilioSid.isBlank()
                && messageRepository.existsByTwilioSid(twilioSid)) {
            log.debug("Duplicate WhatsApp message ignored: {}", twilioSid);
            return;
        }

        String fromNumber = stripWhatsappPrefix(from);

        WhatsappMessage msg = WhatsappMessage.builder()
                .fromNumber(fromNumber)
                .body(body)
                .twilioSid(twilioSid)
                .receivedAt(LocalDateTime.now())
                .build();

        messageRepository.save(msg);
        log.info("WhatsApp message stored from={} sid={}", fromNumber, twilioSid);
    }

    @Transactional(readOnly = true)
    public List<ConversationView> getConversations() {
        return messageRepository.findConversationSummaries().stream()
                .map(this::toConversationView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WhatsappMessage> getThread(String fromNumber) {
        return messageRepository.findByFromNumberOrderByReceivedAtAsc(fromNumber);
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findCustomerByPhone(String fromNumber) {
        String local = toLocalDigits(fromNumber);
        return customerRepository.findByNormalizedPhone(local, "+91" + local);
    }

    private ConversationView toConversationView(ConversationRow row) {
        Optional<Customer> customer = findCustomerByPhone(row.getFromNumber());
        String displayName = customer.map(Customer::getName)
                .orElseGet(() -> formatPhone(row.getFromNumber()));
        Long customerId = customer.map(c -> c.getId()).orElse(null);
        return new ConversationView(
                row.getFromNumber(),
                displayName,
                row.getLastBody(),
                row.getLastReceivedAt(),
                row.getMessageCount() != null ? row.getMessageCount() : 0L,
                row.getUnreadCount() != null ? row.getUnreadCount() : 0L,
                customerId
        );
    }

    /** Strips the "whatsapp:" protocol prefix that Twilio prepends. */
    private static String stripWhatsappPrefix(String phone) {
        if (phone == null) return null;
        return phone.startsWith("whatsapp:") ? phone.substring(9) : phone;
    }

    /**
     * Normalizes any Indian mobile number to its bare 10-digit form.
     * Handles: whatsapp:+91XXXXXXXXXX, +91XXXXXXXXXX, 91XXXXXXXXXX, XXXXXXXXXX.
     */
    public static String toLocalDigits(String phone) {
        if (phone == null) return null;
        String s = stripWhatsappPrefix(phone).replaceAll("[\\s\\-()]", "");
        if (s.startsWith("+91") && s.length() == 13) return s.substring(3);
        if (s.startsWith("91") && s.length() == 12) return s.substring(2);
        return s;
    }

    /** Returns a human-readable display string for a raw phone number. */
    public static String formatPhone(String phone) {
        if (phone == null) return "";
        String s = stripWhatsappPrefix(phone);
        if (s.startsWith("+91") && s.length() == 13) {
            String local = s.substring(3);
            return "+91 " + local.substring(0, 5) + " " + local.substring(5);
        }
        return s;
    }

    public record ConversationView(
            String fromNumber,
            String displayName,
            String lastMessage,
            LocalDateTime lastReceivedAt,
            long messageCount,
            long unreadCount,
            Long customerId
    ) {}
}
