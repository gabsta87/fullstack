package com.serv.services;

import com.serv.common.PasswordPolicy;
import com.serv.database.entities.Email;
import com.serv.database.entities.Worker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the small rule classes that every endpoint relies on. */
class ValidationRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"a@b.co", "first.last@venus.test", "UPPER@Venus.Test", "  spaced@venus.test  "})
    @DisplayName("Email accepts valid addresses and normalizes them")
    void emailValid(String raw) {
        Email email = new Email(raw);
        assertThat(email.getValue()).isEqualTo(raw.trim().toLowerCase());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "plain", "a@", "@b.c", "a@@b.c", "a b@c.d", "a@b..c", "a@b.c d"})
    @DisplayName("Email rejects malformed addresses")
    void emailInvalid(String raw) {
        assertThat(Email.isValid(raw)).isFalse();
        assertThatThrownBy(() -> new Email(raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Email rejects addresses longer than 254 characters")
    void emailTooLong() {
        assertThat(Email.isValid("a".repeat(250) + "@b.co")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Correct-Horse-42", "12345678", "ééééééé8"})
    @DisplayName("Password policy accepts 8+ characters up to 72 bytes")
    void passwordValid(String pw) {
        assertThat(PasswordPolicy.isValid(pw)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"        ", "1234567", "short"})
    @DisplayName("Password policy rejects short or blank passwords")
    void passwordInvalid(String pw) {
        assertThat(PasswordPolicy.isValid(pw)).isFalse();
    }

    @Test
    @DisplayName("Password policy rejects passwords over 72 bytes, counting UTF-8 bytes")
    void passwordTooLong() {
        assertThat(PasswordPolicy.isValid("a".repeat(72))).isTrue();
        assertThat(PasswordPolicy.isValid("a".repeat(73))).isFalse();
        assertThat(PasswordPolicy.isValid("é".repeat(37))).isFalse();
    }

    @Test
    @DisplayName("Birthdate parsing is strict and bounded")
    void birthdate() {
        Worker w = new Worker();
        w.parseAndSetBirthdate("1990-05-17");
        assertThat(w.getAge()).isGreaterThan(30);
        w.parseAndSetBirthdate("17/05/1990");
        assertThat(w.getBirthdate()).isNotNull();
        w.parseAndSetBirthdate("  ");
        assertThat(w.getBirthdate()).isNull();

        for (String bad : new String[]{"1990-02-30", "1990-5-17", "17-05-1990", "2990-01-01", "1700-01-01", "abc"}) {
            assertThatThrownBy(() -> w.parseAndSetBirthdate(bad)).as(bad).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> w.parseAndSetBirthdate(LocalDate.now().plusDays(1).toString()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Banning a user revokes their tokens; changing the password too")
    void tokenRevocation() {
        Worker w = new Worker(new Email("w@venus.test"), "Correct-Horse-42");
        int v0 = w.getTokenVersion();
        w.setBanned(true);
        assertThat(w.getTokenVersion()).isEqualTo(v0 + 1);
        w.setBanned(true);
        assertThat(w.getTokenVersion()).isEqualTo(v0 + 1);
        w.setPassword("Another-Pass-1");
        assertThat(w.getTokenVersion()).isEqualTo(v0 + 2);
        assertThat(w.checkPassword(null)).isFalse();
    }

    @Test
    @DisplayName("RateLimiter allows N events per window then refuses until the window slides")
    void rateLimiter() {
        AtomicLong now = new AtomicLong(1_000_000);
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return Instant.ofEpochMilli(now.get()); }
        };
        RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), clock);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
        assertThat(limiter.tryAcquire("other")).isTrue();

        now.addAndGet(Duration.ofMinutes(1).toMillis() + 1);
        assertThat(limiter.tryAcquire("k")).isTrue();

        now.addAndGet(Duration.ofMinutes(5).toMillis());
        limiter.evictExpired();
        assertThat(limiter.isAllowed("k")).isTrue();
    }
}
