package com.serv.common;

/**
 * Single source of truth for the size and format limits applied to user input.
 * Keep the front-end validators in sync with these values.
 */
public final class InputLimits {

    private InputLimits() {}

    public static final int EMAIL_MAX = 254;
    /** BCrypt only uses the first 72 bytes of a password. */
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX_BYTES = 72;
    /** Upper bound for a password typed at login (never hashed beyond 72 bytes anyway). */
    public static final int PASSWORD_INPUT_MAX = 128;

    public static final int USERNAME_MAX = 50;
    /** Pending "notify me when back" alerts per visitor. */
    public static final int AVAILABILITY_ALERTS_MAX = 200;
    public static final int DESCRIPTION_MAX = 5000;
    public static final int SHORT_DESCRIPTION_MAX = 45;
    /** Digits with an optional leading '+', 6 to 20 characters (same rule as the front). */
    public static final String PHONE_PATTERN = "^$|^(?=.{6,20}$)\\+?[0-9]+$";

    public static final int COMMENT_MAX = 2000;
    public static final int REASON_MAX = 1000;
    public static final int NAME_MAX = 100;
    public static final int LEGAL_CONTENT_MAX = 200_000;
    /** Legal text keys: upper-case words, e.g. ADULT_NOTICE. */
    public static final String LEGAL_KEY_PATTERN = "[A-Z][A-Z_]{0,39}";
    /** Site language codes (front data.json keys). */
    public static final String SITE_LANGUAGE_PATTERN = "fr|en|de|es|pt|it|ru|ua|ar";
    public static final int LIST_MAX = 50;

    public static final int MAX_CREDIT_DAYS = 3650;
}
