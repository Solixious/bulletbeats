package in.bulletbeats.domain.notification;

import java.util.Map;

/**
 * Data required to send a single {@link WhatsappTemplate} notification.
 * Implementations supply the content-variable payload for the Twilio
 * Content Template, plus a plain-text fallback for when no template SID
 * is configured for that key.
 */
public interface TemplateNotification {

    String toPhone();

    NotificationChannel channel();

    /** Maps Twilio content-variable placeholder indices ("1", "2", ...) to values. */
    Map<String, String> templateVariables();

    /** Free-form fallback body used when no template SID is configured. */
    String formattedText();
}
