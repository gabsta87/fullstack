package com.serv.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/** Password rules applied everywhere a password is set (registration, reset, account settings). */
public final class PasswordPolicy {

    private PasswordPolicy() {}

    public static final String MESSAGE = "Password must contain between "
            + InputLimits.PASSWORD_MIN + " and " + InputLimits.PASSWORD_MAX_BYTES + " characters.";

    public static boolean isValid(String password) {
        if (password == null || password.isBlank()) return false;
        if (password.codePointCount(0, password.length()) < InputLimits.PASSWORD_MIN) return false;
        return password.getBytes(StandardCharsets.UTF_8).length <= InputLimits.PASSWORD_MAX_BYTES;
    }

    /** @throws ResponseStatusException 400 if the password does not follow the policy */
    public static void require(String password) {
        if (!isValid(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MESSAGE);
        }
    }
}
