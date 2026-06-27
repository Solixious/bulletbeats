package in.bulletbeats.config;

import in.bulletbeats.domain.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class SetupCheckFilter extends OncePerRequestFilter {

    private static final List<String> STATIC_PATTERNS =
            List.of("/css/**", "/js/**", "/images/**");

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Avoid DB hit for static assets — they are never redirected
        if (isStaticAsset(path)) {
            chain.doFilter(request, response);
            return;
        }

        boolean setupCompleted = userService.isSetupCompleted();

        if (!setupCompleted && !"/setup".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/setup");
            return;
        }

        if (setupCompleted && "/setup".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isStaticAsset(String path) {
        return STATIC_PATTERNS.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
