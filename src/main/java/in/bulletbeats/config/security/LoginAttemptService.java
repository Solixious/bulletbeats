package in.bulletbeats.config.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import in.bulletbeats.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class LoginAttemptService {

    private final Cache<String, Integer> attemptsCache;
    private final AppProperties appProperties;

    public LoginAttemptService(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.attemptsCache = Caffeine.newBuilder()
                .expireAfterWrite(
                        appProperties.getRateLimit().getLoginLockoutMinutes(),
                        TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }

    public void onLoginSuccess(String ip) {
        attemptsCache.invalidate(ip);
        log.debug("Login success — cleared attempts for IP: {}", ip);
    }

    public void onLoginFailure(String ip) {
        int attempts = getAttempts(ip) + 1;
        attemptsCache.put(ip, attempts);
        log.warn("Failed login attempt {} of {} for IP: {}",
                attempts,
                appProperties.getRateLimit().getLoginMaxAttempts(),
                ip);
    }

    public boolean isBlocked(String ip) {
        return getAttempts(ip) >= appProperties.getRateLimit().getLoginMaxAttempts();
    }

    public int getRemainingAttempts(String ip) {
        int max = appProperties.getRateLimit().getLoginMaxAttempts();
        int used = getAttempts(ip);
        return Math.max(0, max - used);
    }

    public int getLockoutMinutes() {
        return appProperties.getRateLimit().getLoginLockoutMinutes();
    }

    public String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        // X-Forwarded-For is comma-separated; first entry is the original client
        return xfHeader.split(",")[0].trim();
    }

    private int getAttempts(String ip) {
        Integer attempts = attemptsCache.getIfPresent(ip);
        return attempts == null ? 0 : attempts;
    }
}
