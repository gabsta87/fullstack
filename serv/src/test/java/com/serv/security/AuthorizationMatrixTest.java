package com.serv.security;

import com.serv.database.entities.Admin;
import com.serv.database.entities.Client;
import com.serv.database.entities.Worker;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every protected endpoint is checked against every kind of caller.
 * When a new endpoint is added, add it here: a missing role check is the most common security bug.
 */
class AuthorizationMatrixTest extends IntegrationTest {

    private static final String ID = UUID.randomUUID().toString();

    private Client client;
    private Worker worker;
    private Admin admin;

    @BeforeEach
    void setUp() {
        client = createClient("client@venus.test");
        worker = createWorker("worker@venus.test");
        admin = createAdmin("admin@venus.test");
    }

    record Endpoint(HttpMethod method, String path) {
        @Override public String toString() { return method + " " + path; }
    }

    static Stream<Endpoint> adminEndpoints() {
        return Stream.of(
                new Endpoint(HttpMethod.GET, "/admin/users"),
                new Endpoint(HttpMethod.DELETE, "/admin/users/" + ID),
                new Endpoint(HttpMethod.GET, "/admin/workers/" + ID),
                new Endpoint(HttpMethod.POST, "/admin/profiles/" + ID + "/status"),
                new Endpoint(HttpMethod.POST, "/admin/profiles/update-days"),
                new Endpoint(HttpMethod.GET, "/admin/certification-requests"),
                new Endpoint(HttpMethod.POST, "/admin/certification-request/1/lock"),
                new Endpoint(HttpMethod.POST, "/admin/certification-request/1/unlock"),
                new Endpoint(HttpMethod.POST, "/admin/profiles/verify-certification"),
                new Endpoint(HttpMethod.POST, "/admin/profiles/revoke-certification"),
                new Endpoint(HttpMethod.POST, "/admin/service"),
                new Endpoint(HttpMethod.DELETE, "/admin/services/1"),
                new Endpoint(HttpMethod.POST, "/admin/legal"),
                new Endpoint(HttpMethod.GET, "/admin/regions-flat"),
                new Endpoint(HttpMethod.DELETE, "/admin/regions/1"),
                new Endpoint(HttpMethod.POST, "/admin/region"),
                new Endpoint(HttpMethod.DELETE, "/admin/comments/1"),
                new Endpoint(HttpMethod.PATCH, "/admin/comments/1"),
                new Endpoint(HttpMethod.DELETE, "/admin/comment/1"),
                new Endpoint(HttpMethod.GET, "/admin/reports-summary"),
                new Endpoint(HttpMethod.POST, "/admin/reports/1/dismiss"),
                new Endpoint(HttpMethod.POST, "/admin/invite"),
                new Endpoint(HttpMethod.GET, "/admin/logs"),
                new Endpoint(HttpMethod.GET, "/admin/certification-photos/" + ID + "/" + ID + ".jpg"),
                new Endpoint(HttpMethod.GET, "/admin/stream"),
                new Endpoint(HttpMethod.GET, "/account/admin/me")
        );
    }

    static Stream<Endpoint> workerEndpoints() {
        return Stream.of(
                new Endpoint(HttpMethod.GET, "/account/worker/me"),
                new Endpoint(HttpMethod.GET, "/account/worker/storage"),
                new Endpoint(HttpMethod.GET, "/account/worker/request-certification"),
                new Endpoint(HttpMethod.POST, "/account/worker/certification-photo"),
                new Endpoint(HttpMethod.PATCH, "/account/worker/availability"),
                new Endpoint(HttpMethod.PATCH, "/account/worker/visibility"),
                new Endpoint(HttpMethod.PATCH, "/account/worker/profile"),
                new Endpoint(HttpMethod.PATCH, "/account/worker/updateservices"),
                new Endpoint(HttpMethod.POST, "/account/worker/media"),
                new Endpoint(HttpMethod.DELETE, "/account/worker/photos/" + ID),
                new Endpoint(HttpMethod.DELETE, "/account/worker/videos/" + ID),
                new Endpoint(HttpMethod.PATCH, "/account/worker/photos/" + ID + "/main"),
                new Endpoint(HttpMethod.PATCH, "/account/worker/photos/reorder"),
                new Endpoint(HttpMethod.POST, "/account/worker/spoken-languages")
        );
    }

    static Stream<Endpoint> clientEndpoints() {
        return Stream.of(
                new Endpoint(HttpMethod.GET, "/account/client/me"),
                new Endpoint(HttpMethod.GET, "/account/client/favorites"),
                new Endpoint(HttpMethod.POST, "/account/client/favorites/" + ID),
                new Endpoint(HttpMethod.DELETE, "/account/client/favorites/" + ID),
                new Endpoint(HttpMethod.GET, "/account/client/filters"),
                new Endpoint(HttpMethod.POST, "/account/client/report-account"),
                new Endpoint(HttpMethod.POST, "/account/client/comment"),
                new Endpoint(HttpMethod.DELETE, "/account/client/comment/1")
        );
    }

    static Stream<Endpoint> authenticatedEndpoints() {
        return Stream.of(
                new Endpoint(HttpMethod.PATCH, "/account/data"),
                new Endpoint(HttpMethod.GET, "/account/stream"),
                new Endpoint(HttpMethod.GET, "/auth/session-check")
        );
    }

    static Stream<Endpoint> closedEndpoints() {
        return Stream.of(
                new Endpoint(HttpMethod.GET, "/payment/config"),
                new Endpoint(HttpMethod.POST, "/payment/create-payment-intent"),
                new Endpoint(HttpMethod.POST, "/payment/webhook"),
                new Endpoint(HttpMethod.POST, "/media/" + ID + "/media"),
                new Endpoint(HttpMethod.GET, "/does-not-exist")
        );
    }

    private MockHttpServletRequestBuilder call(Endpoint e, String authorization) {
        MockHttpServletRequestBuilder req = request(e.method(), e.path())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
        if (authorization != null) req.header("Authorization", authorization);
        return req;
    }

    private int statusOf(Endpoint e, String authorization) throws Exception {
        return mvc.perform(call(e, authorization)).andReturn().getResponse().getStatus();
    }

    // ── Admin area ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpoints")
    @DisplayName("Admin endpoints: anonymous -> 401")
    void adminEndpoints_rejectAnonymous(Endpoint e) throws Exception {
        assertThat(statusOf(e, null)).isEqualTo(401);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpoints")
    @DisplayName("Admin endpoints: client -> 403")
    void adminEndpoints_rejectClient(Endpoint e) throws Exception {
        assertThat(statusOf(e, bearer(client))).isEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpoints")
    @DisplayName("Admin endpoints: worker -> 403")
    void adminEndpoints_rejectWorker(Endpoint e) throws Exception {
        assertThat(statusOf(e, bearer(worker))).isEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adminEndpoints")
    @DisplayName("Admin endpoints: admin passes the security layer")
    void adminEndpoints_allowAdmin(Endpoint e) throws Exception {
        if (e.path().endsWith("/stream")) return; // long-lived response, covered elsewhere
        assertThat(statusOf(e, bearer(admin))).isNotIn(401, 403);
    }

    // ── Worker area ──────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("workerEndpoints")
    @DisplayName("Worker endpoints: anonymous -> 401, client/admin -> 403")
    void workerEndpoints_onlyWorkers(Endpoint e) throws Exception {
        assertThat(statusOf(e, null)).isEqualTo(401);
        assertThat(statusOf(e, bearer(client))).isEqualTo(403);
        assertThat(statusOf(e, bearer(admin))).isEqualTo(403);
        assertThat(statusOf(e, bearer(worker))).isNotIn(401, 403);
    }

    // ── Client area ──────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("clientEndpoints")
    @DisplayName("Client endpoints: anonymous -> 401, worker/admin -> 403")
    void clientEndpoints_onlyClients(Endpoint e) throws Exception {
        assertThat(statusOf(e, null)).isEqualTo(401);
        assertThat(statusOf(e, bearer(worker))).isEqualTo(403);
        assertThat(statusOf(e, bearer(admin))).isEqualTo(403);
        assertThat(statusOf(e, bearer(client))).isNotIn(401, 403);
    }

    // ── Any authenticated user ───────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("authenticatedEndpoints")
    @DisplayName("Account endpoints require a valid token")
    void accountEndpoints_requireAuthentication(Endpoint e) throws Exception {
        assertThat(statusOf(e, null)).isEqualTo(401);
        assertThat(statusOf(e, "Bearer not-a-jwt")).isEqualTo(401);
    }

    // ── Closed ───────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0}")
    @MethodSource("closedEndpoints")
    @DisplayName("Payment, legacy public upload and unknown routes are closed to everyone")
    void closedEndpoints_denyEveryone(Endpoint e) throws Exception {
        assertThat(statusOf(e, null)).isIn(401, 403);
        assertThat(statusOf(e, bearer(client))).isEqualTo(403);
        assertThat(statusOf(e, bearer(admin))).isEqualTo(403);
    }

    @Test
    @DisplayName("Public read endpoints stay open to anonymous visitors")
    void publicEndpoints_open() throws Exception {
        assertThat(statusOf(new Endpoint(HttpMethod.GET, "/workers"), null)).isEqualTo(200);
        assertThat(statusOf(new Endpoint(HttpMethod.GET, "/common/services"), null)).isEqualTo(200);
        assertThat(statusOf(new Endpoint(HttpMethod.GET, "/common/regions"), null)).isEqualTo(200);
        // No legal text in the emptied test database: 404, but never refused to anonymous visitors
        assertThat(statusOf(new Endpoint(HttpMethod.GET, "/common/legal/ADULT_NOTICE"), null)).isNotIn(401, 403);
    }

    @Test
    @DisplayName("A super admin has every admin right")
    void superAdmin_hasAdminRights() throws Exception {
        var superAdmin = createSuperAdmin("root@venus.test");
        assertThat(statusOf(new Endpoint(HttpMethod.GET, "/admin/users"), bearer(superAdmin))).isEqualTo(200);
        assertThat(statusOf(new Endpoint(HttpMethod.GET, "/account/admin/me"), bearer(superAdmin))).isEqualTo(200);
    }
}
