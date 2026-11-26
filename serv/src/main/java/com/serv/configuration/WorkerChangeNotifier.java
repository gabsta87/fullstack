package com.serv.configuration;

import com.serv.services.SseStreamService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * After an advertiser changed something on their own profile ({@code /account/worker/**}, any method but
 * GET, success only), tells the admins which profile changed, so an admin looking at that profile sees it
 * at once. Runs after the request, so after the commit. Only the id is sent, never the content.
 */
@Component
@RequiredArgsConstructor
public class WorkerChangeNotifier implements HandlerInterceptor {

    public static final String EVENT = "WORKER_UPDATED";

    private final SseStreamService sseStreamService;

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        if ("GET".equalsIgnoreCase(request.getMethod()) || ex != null) return;
        if (response.getStatus() < 200 || response.getStatus() >= 300) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt && jwt.getClaimAsString("userId") != null) {
            sseStreamService.broadcastAdminEvent(EVENT, jwt.getClaimAsString("userId"));
        }
    }
}
