package com.serv.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Gives every request a short id (MDC {@code requestId}, response header {@code X-Request-Id})
 * so all log lines of one request can be grepped together, and logs only the requests worth
 * looking at: server errors, slow requests and refused access. Never logs query strings
 * (the SSE token travels there) nor request bodies.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    static final long SLOW_REQUEST_MS = 2000;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            logOutcome(request, response.getStatus(), System.currentTimeMillis() - start);
            MDC.remove(REQUEST_ID);
        }
    }

    private void logOutcome(HttpServletRequest request, int status, long durationMs) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (status >= 500) {
            log.error("{} {} -> {} ({} ms) from {}", method, path, status, durationMs, clientIp(request));
        } else if (isStream(path)) {
            return; // SSE connections stay open for minutes: their duration is not a slowness signal
        } else if (durationMs > SLOW_REQUEST_MS) {
            log.warn("Slow request {} {} -> {} ({} ms)", method, path, status, durationMs);
        } else if (status == 403 || status == 429 || (status == 401 && !"GET".equals(method))) {
            // Anonymous GETs answered 401 (e.g. session check of a visitor) are normal traffic.
            log.info("Refused {} {} -> {} from {}", method, path, status, clientIp(request));
        }
    }

    private static boolean isStream(String path) {
        return path.endsWith("/stream");
    }

    /** nginx sets X-Real-IP; falls back to the socket address. */
    static String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return (realIp != null && !realIp.isBlank()) ? realIp : request.getRemoteAddr();
    }
}
