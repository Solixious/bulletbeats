package in.bulletbeats.config.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthenticationEventListener
        implements ApplicationListener<AbstractAuthenticationEvent> {

    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        String ip = loginAttemptService.getClientIp(request);

        if (event instanceof AuthenticationSuccessEvent) {
            loginAttemptService.onLoginSuccess(ip);
        }

        if (event instanceof AbstractAuthenticationFailureEvent failure) {
            loginAttemptService.onLoginFailure(ip);
            log.warn("Authentication failure for IP: {} — reason: {}",
                    ip, failure.getException().getMessage());
        }
    }
}
