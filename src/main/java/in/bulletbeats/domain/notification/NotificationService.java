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
            log.info("Twilio initialized (templates configured={})", properties.getTemplates().keySet());
        }
    }

    public boolean isEnabled() {
        return appConfigService.getBoolean("notification.enabled", false);
    }

    public boolean isConfigured() {
        return hasText(properties.getAccountSid()) && hasText(properties.getAuthToken());
    }

    public boolean hasTemplate(WhatsappTemplate template) {
        return hasText(properties.getTemplateSid(template));
    }

    /**
     * Auto-send on trigger (e.g. payment) — silent, checks the notification.enabled flag.
     */
    public void send(WhatsappTemplate template, TemplateNotification data) {
        if (!isEnabled()) {
            log.debug("Notification skipped — notification.enabled is false");
            return;
        }
        if (!isConfigured()) {
            log.warn("Notification skipped — Twilio credentials not configured");
            return;
        }
        try {
            dispatch(template, data);
        } catch (Exception e) {
            log.error("Failed to send {} {} notification to {}: {}",
                    data.channel(), template, data.toPhone(), e.getMessage());
        }
    }

    /**
     * Explicit manual send (staff-triggered button) — throws on any error,
     * bypasses the notification.enabled flag, still requires configuration.
     */
    public void sendNow(WhatsappTemplate template, TemplateNotification data) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Twilio not configured — set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, "
                    + "TWILIO_WHATSAPP_FROM, and TWILIO_CONTENT_SID");
        }
        dispatch(template, data);
    }

    private void dispatch(WhatsappTemplate template, TemplateNotification data) {
        if (hasTemplate(template)) {
            doSendTemplate(template, data);
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

    private void doSendTemplate(WhatsappTemplate template, TemplateNotification data) {
        String contentSid = properties.getTemplateSid(template);
        String from = whatsappFrom();
        String to = "whatsapp:" + normalizePhone(data.toPhone());

        String vars = serializeVars(data.templateVariables());
        log.info("Sending template {} ({}) — from={} to={} vars={}",
                template, contentSid, from, to, vars);

        Message msg = Message.creator(new PhoneNumber(to), new PhoneNumber(from), "")
                .setContentSid(contentSid)
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

    private String serializeVars(Map<String, String> vars) {
        try {
            return OBJECT_MAPPER.writeValueAsString(vars);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize template variables", e);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
