package in.bulletbeats.domain.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
}
