package com.serv.common;

/**
 * Names of all the database tables, in one place.
 * Never rename an existing value: with {@code ddl-auto=update}, Hibernate would create a new,
 * empty table and leave the data in the old one.
 */
public final class TableNames {

    private TableNames() {}

    // Users (JOINED inheritance: one table per class)
    public static final String USERS = "users";
    public static final String CLIENTS = "clients";
    public static final String WORKERS = "workers";
    public static final String ADMINS = "admin";
    public static final String SUPER_ADMINS = "super_admin";

    // Worker data
    public static final String PHOTOS = "photos";
    public static final String VIDEOS = "videos";
    public static final String WORKER_SERVICES = "workers_services";
    public static final String WORKER_LANGUAGES = "workers_languages";
    public static final String CERTIFICATION_REQUESTS = "certification_requests";

    // Client data
    public static final String CLIENT_FAVORITES = "client_favorites";
    public static final String CLIENT_PREFERRED_SERVICES = "client_preferred_services";
    public static final String CLIENT_PREFERRED_LANGUAGES = "client_preferred_languages";
    public static final String COMMENTS = "comments";
    public static final String REPORTS = "reports";
    public static final String AVAILABILITY_ALERTS = "availability_alerts";

    // Reference data
    public static final String SERVICES = "services";
    public static final String GEOGRAPHIC_ZONES = "geographic_zones";
    public static final String LOCATIONS = "locations";
    public static final String LEGAL_TEXTS = "legal_text";

    // Tokens, payments, audit
    public static final String VERIFICATION_TOKENS = "verification_tokens";
    public static final String PASSWORD_RESET_TOKENS = "password_reset_token";
    public static final String PAYMENTS = "payments";
    public static final String ADMIN_AUDIT_LOGS = "admin_audit_logs";
}
