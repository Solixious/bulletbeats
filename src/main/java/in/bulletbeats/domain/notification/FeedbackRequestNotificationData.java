package in.bulletbeats.domain.notification;

import java.util.LinkedHashMap;
import java.util.Map;

public record FeedbackRequestNotificationData(
        String toPhone,
        NotificationChannel channel,
        String customerName,    // template {{1}}
        String cafeName,        // template {{2}}
        String formattedText    // free-form fallback when no template configured
) implements TemplateNotification {

    /**
     * Variables match the approved WhatsappTemplate#FEEDBACK_REQUEST placeholders:
     *   {{1}} = customer name
     *   {{2}} = cafe name
     *
     * Template body:
     *   Hi {{1}}, thanks for visiting {{2}}! We'd love your feedback — just reply
     *   to this message and let us know how it went.
     */
    @Override
    public Map<String, String> templateVariables() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("1", orEmpty(customerName));
        vars.put("2", orEmpty(cafeName));
        return vars;
    }

    private static String orEmpty(String s) {
        return s != null ? s : "";
    }
}
