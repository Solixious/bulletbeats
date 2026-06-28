package in.bulletbeats.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app")
@Component
@Getter
@Setter
public class AppProperties {

    private String baseUrl = "http://localhost:8080";
    private int maxImageSizeMb = 2;
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit {
        private int loginMaxAttempts = 5;
        private int loginLockoutMinutes = 15;
    }
}
