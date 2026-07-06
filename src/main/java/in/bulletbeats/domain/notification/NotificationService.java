package in.bulletbeats.domain.notification;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import in.bulletbeats.domain.admin.AppConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AppConfigService appConfigService;
    private final TwilioProperties properties;

    @PostConstruct
    void initTwilio() {
        if (isConfigured()) {
            Twilio.init(properties.getAccountSid(), properties.getAuthToken());
            log.info("Twilio initialized");
        }
    }

    public boolean isEnabled() {
        return appConfigService.getBoolean("notification.enabled", false);
    }

    public boolean isConfigured() {
        return hasText(properties.getAccountSid()) && hasText(properties.getAuthToken());
    }

    /**
     * Send a notification silently — errors are logged, not thrown.
     * Respects the notification.enabled flag.
     */
    public void sendBillNotification(String toPhone, String message, NotificationChannel channel) {
        if (!isEnabled() || !isConfigured()) return;
        try {
            doSend(toPhone, message, channel);
        } catch (Exception e) {
            log.error("Failed to send {} notification to {}: {}", channel, toPhone, e.getMessage());
        }
    }

    /**
     * Send a notification immediately — throws on any error.
     * Ignores the notification.enabled flag (used for admin test sends).
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

    private void doSend(String toPhone, String message, NotificationChannel channel) {
        String from, to;
        if (channel == NotificationChannel.WHATSAPP) {
            String raw = properties.getWhatsappFrom();
            from = raw.startsWith("whatsapp:") ? raw : "whatsapp:" + raw;
            to = "whatsapp:" + toPhone;
        } else {
            from = properties.getFromNumber();
            to = toPhone;
        }
        log.info("Sending {} — from={} to={}", channel, from, to);
        Message msg = Message.creator(new PhoneNumber(to), new PhoneNumber(from), message).create();
        log.info("Twilio response — SID={} Status={} ErrorCode={} ErrorMessage={}",
                msg.getSid(), msg.getStatus(), msg.getErrorCode(), msg.getErrorMessage());
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
