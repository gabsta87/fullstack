package com.serv.services;

import com.serv.database.entities.Email;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

/**
 * Throttles sensitive authentication flows per account (email), which cannot be bypassed by
 * rotating IP addresses or spoofing proxy headers:
 * <ul>
 *   <li>login: 5 failed attempts per 15 minutes, then the account refuses logins until the window passes;</li>
 *   <li>password reset requests: 3 per hour (prevents mail bombing).</li>
 * </ul>
 * IP-based throttling (mass registration, scraping) belongs to the reverse proxy.
 */
@Service
public class AbuseProtectionService {

    public static final int MAX_LOGIN_FAILURES = 5;
    public static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    public static final int MAX_RESET_REQUESTS = 3;
    public static final Duration RESET_WINDOW = Duration.ofHours(1);

    private final RateLimiter loginFailures;
    private final RateLimiter resetRequests;

    public AbuseProtectionService() {
        this(Clock.systemUTC());
    }

    AbuseProtectionService(Clock clock) {
        this.loginFailures = new RateLimiter(MAX_LOGIN_FAILURES, LOGIN_WINDOW, clock);
        this.resetRequests = new RateLimiter(MAX_RESET_REQUESTS, RESET_WINDOW, clock);
    }

    public boolean isLoginBlocked(String email) {
        return !loginFailures.isAllowed(key(email));
    }

    public void recordLoginFailure(String email) {
        loginFailures.record(key(email));
    }

    public void recordLoginSuccess(String email) {
        loginFailures.reset(key(email));
    }

    /** @return false if too many reset emails were requested for this address recently */
    public boolean tryPasswordResetRequest(String email) {
        return resetRequests.tryAcquire(key(email));
    }

    public void clearAll() {
        loginFailures.clear();
        resetRequests.clear();
    }

    @Scheduled(fixedRate = 10 * 60 * 1000L)
    public void evictExpired() {
        loginFailures.evictExpired();
        resetRequests.evictExpired();
    }

    private static String key(String email) {
        String normalized = Email.normalize(email);
        return normalized == null ? "" : normalized;
    }
}
