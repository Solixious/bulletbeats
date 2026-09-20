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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String TELEGRAM_TEMPLATE_ORDER_RECEIVED_DINE_IN = """
            New dine-in order via QR at Bullet Beats Cafe.

            *Order Details*
            • Bill Number: #{{1}}
            • Customer: {{2}}
            • Phone: {{3}}
            • Table: {{4}}

            *Items*
            {{5}}

            *Total*: {{6}}

            Thank you!""";

    private static final String TELEGRAM_TEMPLATE_ORDER_RECEIVED_DELIVERY = """
            New direct delivery order at Bullet Beats Cafe.

            • Bill Number: #{{1}}
            • Customer: {{2}}
            • Phone: {{3}}
            • Address: {{4}}

            *Items*
            {{5}}

            *Total*: {{6}}

            Thank you!""";

    private final RestClient restClient = RestClient.create();

    private final AppConfigService appConfigService;
    private final TwilioProperties properties;
    private final TelegramProperties telegramProperties;

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

    public boolean isTelegramConfigured() {
        return hasText(telegramProperties.getBotToken()) && hasText(telegramProperties.getStaffChatId());
    }

    /** Whether staff order alerts should still go out over WhatsApp (in addition to Telegram). */
    public boolean isWhatsappStaffEnabled() {
        return appConfigService.getBoolean("notification.whatsapp.staff.enabled", true);
    }

    /**
     * Auto-send on trigger (e.g. payment) — silent, checks the notification.enabled flag.
     */
    public void send(WhatsappTemplate template, TemplateNotification data) {
        if (!isEnabled()) {
            log.debug("Notification skipped — notification.enabled is false");
            return;
        }
        if (data.channel() == NotificationChannel.WHATSAPP
                && template.getAudience() == TemplateAudience.STAFF
                && !isWhatsappStaffEnabled()) {
            log.debug("WhatsApp skipped for staff template {} — notification.whatsapp.staff.enabled is false", template);
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
     * Auto-send a staff alert to the shared Telegram staff chat — silent, checks the
     * notification.enabled flag. Independent of the WhatsApp staff toggle: both
     * channels can run side by side while reliability is being evaluated.
     */
    public void sendStaffTelegram(WhatsappTemplate template, TemplateNotification data) {
        if (!isEnabled()) {
            log.debug("Telegram notification skipped — notification.enabled is false");
            return;
        }
        if (!isTelegramConfigured()) {
            log.debug("Telegram notification skipped — bot token / staff chat id not configured");
            return;
        }
        try {
            String templateText = telegramTemplateFor(template);
            if (templateText != null) {
                doSendTelegram(renderTelegramTemplate(templateText, data), true);
            } else {
                doSendTelegram(data.formattedText(), false);
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram {} staff notification: {}", template, e.getMessage());
        }
    }

    /**
     * Explicit manual test send to the staff Telegram chat — throws on any error,
     * bypasses the notification.enabled flag, still requires configuration.
     */
    public void testSendTelegram(String message) {
        if (!isTelegramConfigured()) {
            throw new IllegalStateException(
                    "Telegram not configured — set TELEGRAM_BOT_TOKEN and TELEGRAM_STAFF_CHAT_ID");
        }
        doSendTelegram(message, false);
        log.info("Test Telegram notification sent to staff chat");
    }

    private static String telegramTemplateFor(WhatsappTemplate template) {
        return switch (template) {
            case ORDER_RECEIVED_DINE_IN -> TELEGRAM_TEMPLATE_ORDER_RECEIVED_DINE_IN;
            case ORDER_RECEIVED_DELIVERY -> TELEGRAM_TEMPLATE_ORDER_RECEIVED_DELIVERY;
            default -> null;
        };
    }

    private static String renderTelegramTemplate(String templateText, TemplateNotification data) {
        String rendered = templateText;
        for (Map.Entry<String, String> entry : data.templateVariables().entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", escapeMarkdown(entry.getValue()));
        }
        return rendered;
    }

    /** Escapes Telegram legacy-Markdown entity characters in interpolated (non-template) text. */
    private static String escapeMarkdown(String s) {
        return s.replaceAll("([_*\\[`])", "\\\\$1");
    }

    private void doSendTelegram(String text, boolean markdown) {
        String url = "https://api.telegram.org/bot" + telegramProperties.getBotToken() + "/sendMessage";
        Map<String, Object> body = markdown
                ? Map.of("chat_id", telegramProperties.getStaffChatId(), "text", text, "parse_mode", "Markdown")
                : Map.of("chat_id", telegramProperties.getStaffChatId(), "text", text);
        String response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        log.info("Telegram response: {}", response);
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
