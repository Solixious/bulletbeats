package in.bulletbeats.domain.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "telegram")
@Component
@Getter
@Setter
public class TelegramProperties {

    /** Telegram Bot API token — set via TELEGRAM_BOT_TOKEN env var */
    private String botToken = "";

    /** Chat ID of the staff group/channel the bot posts into — set via TELEGRAM_STAFF_CHAT_ID */
    private String staffChatId = "";
}
