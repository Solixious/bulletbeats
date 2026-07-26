package in.bulletbeats.domain.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "twilio")
@Component
@Getter
@Setter
public class TwilioProperties {

    /** Twilio Account SID — set via TWILIO_ACCOUNT_SID env var */
    private String accountSid = "";

    /** Twilio Auth Token — set via TWILIO_AUTH_TOKEN env var */
    private String authToken = "";

    /** Phone number for SMS (E.164, e.g. +14155238886) — set via TWILIO_FROM_NUMBER */
    private String fromNumber = "";

    /** WhatsApp sender (E.164, e.g. +14155238886) — set via TWILIO_WHATSAPP_FROM */
    private String whatsappFrom = "";

    /**
     * Content Template SIDs (HX...), keyed by {@link WhatsappTemplate#getConfigKey()}
     * — set via TWILIO_TEMPLATES_&lt;KEY&gt; env vars, e.g. TWILIO_TEMPLATES_BILL_RECEIPT.
     */
    private Map<String, String> templates = new HashMap<>();

    public String getTemplateSid(WhatsappTemplate template) {
        return templates.get(template.getConfigKey());
    }
}
