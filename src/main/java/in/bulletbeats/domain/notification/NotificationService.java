package in.bulletbeats.domain.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import in.bulletbeats.domain.admin.AppConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AppConfigService appConfigService;
    private final TwilioProperties properties;

    @PostConstruct
    void initTwilio() {
        if (isConfigured()) {
            Twilio.init(properties.getAccountSid(), properties.getAuthToken());
            log.info("Twilio initialized (template={})", hasTemplate() ? properties.getContentSid() : "none");
        }
    }

    public boolean isEnabled() {
        return appConfigService.getBoolean("notification.enabled", false);
    }

    public boolean isConfigured() {
        return hasText(properties.getAccountSid()) && hasText(properties.getAuthToken());
    }

    public boolean hasTemplate() {
        return hasText(properties.getContentSid());
    }

    /**
     * Auto-send on payment — silent, checks the notification.enabled flag.
     */
    public void sendBillNotification(BillNotificationData data) {
        if (!isEnabled()) {
            log.debug("Notification skipped — notification.enabled is false");
            return;
        }
        if (!isConfigured()) {
            log.warn("Notification skipped — Twilio credentials not configured");
            return;
        }
        try {
            dispatchBill(data);
        } catch (Exception e) {
            log.error("Failed to send {} bill notification to {}: {}",
                    data.channel(), data.toPhone(), e.getMessage());
        }
    }

    /**
     * Explicit manual send (staff-triggered button) — throws on any error,
     * bypasses the notification.enabled flag, still requires configuration.
     */
    public void sendBillNotificationNow(BillNotificationData data) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Twilio not configured — set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, "
                    + "TWILIO_WHATSAPP_FROM, and TWILIO_CONTENT_SID");
        }
        dispatchBill(data);
    }

    private void dispatchBill(BillNotificationData data) {
        if (hasTemplate()) {
            doSendTemplate(data);
        } else {
            doSend(data.toPhone(), data.formattedText(), data.channel());
        }
    }

    /**
     * Send a free-form test message immediately — throws on any error.
     * Ignores the notification.enabled flag (admin use only).
     */
    public void testSend(String toPhone, String message, NotificationChannel channel) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Twilio not configured — set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, "
                    + "TWILIO_FROM_NUMBER, and TWILIO_WHATSAPP_FROM");
        }
        doSend(toPhone, message, channel);
        log.info("Test notification sent via {} to {}", channel, toPhone);
    }

    private void doSendTemplate(BillNotificationData data) {
        String from = whatsappFrom();
        String to = "whatsapp:" + normalizePhone(data.toPhone());

        String vars = buildTemplateVars(data);
        log.info("Sending template {} — from={} to={} vars={}", properties.getContentSid(), from, to, vars);

        Message msg = Message.creator(new PhoneNumber(to), new PhoneNumber(from), "")
                .setContentSid(properties.getContentSid())
                .setContentVariables(vars)
                .create();

        log.info("Twilio response — SID={} Status={} ErrorCode={} ErrorMessage={}",
                msg.getSid(), msg.getStatus(), msg.getErrorCode(), msg.getErrorMessage());
    }

    private void doSend(String toPhone, String message, NotificationChannel channel) {
        String from, to;
        String normalized = normalizePhone(toPhone);
        if (channel == NotificationChannel.WHATSAPP) {
            from = whatsappFrom();
            to = "whatsapp:" + normalized;
        } else {
            from = properties.getFromNumber();
            to = normalized;
        }
        log.info("Sending {} — from={} to={}", channel, from, to);
        Message msg = Message.creator(new PhoneNumber(to), new PhoneNumber(from), message).create();
        log.info("Twilio response — SID={} Status={} ErrorCode={} ErrorMessage={}",
                msg.getSid(), msg.getStatus(), msg.getErrorCode(), msg.getErrorMessage());
    }

    private String whatsappFrom() {
        String raw = properties.getWhatsappFrom();
        return raw.startsWith("whatsapp:") ? raw : "whatsapp:" + raw;
    }

    /** Ensures the number is in E.164 format. Bare 10-digit numbers get +91 prepended. */
    private static String normalizePhone(String phone) {
        if (phone == null) return phone;
        String digits = phone.replaceAll("[\\s\\-()]", "");
        if (digits.startsWith("+")) return digits;
        if (digits.startsWith("91") && digits.length() == 12) return "+" + digits;
        if (digits.length() == 10) return "+91" + digits;
        return digits;
    }

    /**
     * Variables match the approved template placeholders:
     *   {{1}} = customer name
     *   {{2}} = cafe name
     *   {{3}} = full bill breakdown (bill#, date, items, subtotal, GST, total, payment)
     *
     * Template body:
     *   Hi {{1}}, here is your bill from {{2}}.
     *
     *   {{3}}
     *
     *   Thank you for visiting!
     */
    private String buildTemplateVars(BillNotificationData data) {
        try {
            // WhatsApp template variable values must not contain newlines — flatten to single line
            String breakdown = data.breakdown() != null
                    ? data.breakdown().replace("\n", " ").replaceAll(" {2,}", " ").trim()
                    : "";
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                    "1", data.customerName() != null ? data.customerName() : "",
                    "2", data.cafeName() != null ? data.cafeName() : "",
                    "3", breakdown
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize template variables", e);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
