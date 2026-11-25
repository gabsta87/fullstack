package com.serv.security;

import com.serv.database.entities.*;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessControlTest extends IntegrationTest {

    // ── Public profiles ──────────────────────────────────────────────────────

    @Nested
    class PublicProfiles {

        private Worker hiddenBy(Consumer<Worker> change) {
            Worker w = createVisibleWorker("w" + UUID.randomUUID().toString().substring(0, 8) + "@venus.test");
            Worker fresh = workerRepository.findById(w.getId()).orElseThrow();
            change.accept(fresh);
            return workerRepository.save(fresh);
        }

        @Test
        @DisplayName("A complete, active profile is public")
        void visibleProfile() throws Exception {
            Worker w = createVisibleWorker("luna@venus.test");
            mvc.perform(get("/workers/" + w.getId())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").doesNotExist())
                    .andExpect(jsonPath("$.verificationCode").doesNotExist())
                    .andExpect(jsonPath("$.birthdate").doesNotExist());
        }

        @Test
        @DisplayName("Banned, locked, hidden, expired, incomplete and minor profiles are not public")
        void nonPublicProfiles() throws Exception {
            for (Worker w : new Worker[]{
                    hiddenBy(x -> x.setBanned(true)),
                    hiddenBy(x -> x.setLocked(true)),
                    hiddenBy(x -> x.setHidden(true)),
                    hiddenBy(x -> x.setExpired(true)),
                    hiddenBy(x -> x.setInvalid(true)),
                    hiddenBy(x -> x.setBirthdate(java.sql.Date.valueOf(java.time.LocalDate.now().minusYears(17))))}) {
                mvc.perform(get("/workers/" + w.getId())).andExpect(status().isNotFound());
                mvc.perform(get("/workers/" + w.getId()).header("Authorization", bearer(createClient(
                        "c" + UUID.randomUUID().toString().substring(0, 8) + "@venus.test")))).andExpect(status().isNotFound());
            }
        }

        @Test
        @DisplayName("Admins can still open a non-public profile; so can its owner")
        void adminAndOwnerCanSee() throws Exception {
            Worker banned = hiddenBy(x -> x.setHidden(true));
            mvc.perform(get("/workers/" + banned.getId()).header("Authorization", bearer(createAdmin("admin@venus.test"))))
                    .andExpect(status().isOk());
            mvc.perform(get("/workers/" + banned.getId()).header("Authorization", bearer(banned)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Gallery only lists public profiles")
        void galleryFilters() throws Exception {
            Worker visible = createVisibleWorker("luna@venus.test");
            hiddenBy(x -> x.setBanned(true));
            createWorker("incomplete@venus.test");
            mvc.perform(get("/workers")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(visible.getId().toString()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"page=-5", "page=abc", "minAge=abc", "zoneId=undefined", "bodyType=%27%20OR%201%3D1",
                "username=%25%25%25", "username=%27%3B%20DROP%20TABLE%20users%3B--"})
        @DisplayName("Odd gallery filters never cause a server error")
        void galleryOddFilters(String query) throws Exception {
            int code = mvc.perform(get("/workers?" + query)).andReturn().getResponse().getStatus();
            assertThat(code).isIn(200, 400);
        }

        @Test
        @DisplayName("Gallery rejects absurdly long search strings")
        void galleryLongSearch() throws Exception {
            mvc.perform(get("/workers").param("username", "x".repeat(500))).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Unknown or malformed profile ids give 404 / 400, never 500")
        void badIds() throws Exception {
            mvc.perform(get("/workers/" + UUID.randomUUID())).andExpect(status().isNotFound());
            mvc.perform(get("/workers/not-a-uuid")).andExpect(status().isBadRequest());
        }
    }

    // ── Clients ──────────────────────────────────────────────────────────────

    @Nested
    class Clients {

        @Test
        @DisplayName("Comments are validated and trimmed")
        void commentValidation() throws Exception {
            Client c = createClient("client@venus.test");
            Worker w = createVisibleWorker("luna@venus.test");
            String url = "/account/client/comment";

            for (String body : new String[]{
                    toJson(Map.of("content", "", "workerId", w.getId().toString())),
                    toJson(Map.of("content", "   ", "workerId", w.getId().toString())),
                    toJson(Map.of("content", "x".repeat(2001), "workerId", w.getId().toString())),
                    toJson(Map.of("content", "Hello", "workerId", "not-a-uuid")),
                    toJson(Map.of("content", "Hello")),
                    "{}"}) {
                mvc.perform(post(url).header("Authorization", bearer(c)).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest());
            }
            assertThat(commentRepository.count()).isZero();

            mvc.perform(post(url).header("Authorization", bearer(c)).contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("content", "  Great  ", "workerId", w.getId().toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("Great"));
        }

        @Test
        @DisplayName("Comments cannot target a hidden or banned profile")
        void commentOnHiddenProfile() throws Exception {
            Client c = createClient("client@venus.test");
            Worker w = createVisibleWorker("luna@venus.test");
            Worker fresh = workerRepository.findById(w.getId()).orElseThrow();
            fresh.setBanned(true);
            workerRepository.save(fresh);
            mvc.perform(post("/account/client/comment").header("Authorization", bearer(c)).contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("content", "Hello", "workerId", w.getId().toString()))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("A client cannot delete another client's comment")
        void cannotDeleteOthersComment() throws Exception {
            Client author = createClient("author@venus.test");
            Client attacker = createClient("attacker@venus.test");
            Worker w = createVisibleWorker("luna@venus.test");
            Comment comment = commentRepository.save(new Comment(author, w, "Mine"));

            mvc.perform(delete("/account/client/comment/" + comment.getId()).header("Authorization", bearer(attacker)))
                    .andExpect(status().isForbidden());
            assertThat(commentRepository.findById(comment.getId())).isPresent();
        }

        @Test
        @DisplayName("Reports are validated")
        void reportValidation() throws Exception {
            Client c = createClient("client@venus.test");
            Worker w = createVisibleWorker("luna@venus.test");
            for (String body : new String[]{
                    toJson(Map.of("reason", "", "workerId", w.getId().toString())),
                    toJson(Map.of("reason", "x".repeat(1001), "workerId", w.getId().toString())),
                    toJson(Map.of("reason", "Fake", "workerId", "abc")),
                    "{}"}) {
                mvc.perform(post("/account/client/report-account").header("Authorization", bearer(c))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
            }
        }

        @Test
        @DisplayName("Favorites only accept public profiles")
        void favoritesOnlyPublic() throws Exception {
            Client c = createClient("client@venus.test");
            Worker incomplete = createWorker("incomplete@venus.test");
            mvc.perform(post("/account/client/favorites/" + incomplete.getId()).header("Authorization", bearer(c)))
                    .andExpect(status().isNotFound());
            mvc.perform(post("/account/client/favorites/" + UUID.randomUUID()).header("Authorization", bearer(c)))
                    .andExpect(status().isNotFound());
        }
    }

    // ── Account settings ─────────────────────────────────────────────────────

    @Nested
    class AccountSettings {

        private org.springframework.test.web.servlet.ResultActions patchData(VenusUser user, Map<String, ?> body) throws Exception {
            return mvc.perform(patch("/account/data").header("Authorization", bearer(user))
                    .contentType(MediaType.APPLICATION_JSON).content(toJson(body)));
        }

        @Test
        @DisplayName("Changing the password requires the current password")
        void passwordNeedsOld() throws Exception {
            Client c = createClient("client@venus.test");
            patchData(c, Map.of("password", "New-Password-1")).andExpect(status().isBadRequest());
            patchData(c, Map.of("password", "New-Password-1", "oldPassword", "wrong")).andExpect(status().isUnauthorized());
            assertThat(reload(c).checkPassword(PASSWORD)).isTrue();
        }

        @Test
        @DisplayName("A password change revokes old tokens and returns a new one")
        void passwordChangeRotatesToken() throws Exception {
            Client c = createClient("client@venus.test");
            String oldAuth = bearer(c);
            String newToken = patchData(c, Map.of("password", "New-Password-1", "oldPassword", PASSWORD))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Auth-Token"))
                    .andReturn().getResponse().getHeader("X-Auth-Token");

            mvc.perform(get("/auth/session-check").header("Authorization", oldAuth)).andExpect(status().isUnauthorized());
            mvc.perform(get("/auth/session-check").header("Authorization", "Bearer " + newToken)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("The new password must follow the policy")
        void passwordPolicy() throws Exception {
            Client c = createClient("client@venus.test");
            patchData(c, Map.of("password", "short", "oldPassword", PASSWORD)).andExpect(status().isBadRequest());
            assertThat(reload(c).checkPassword(PASSWORD)).isTrue();
        }

        @Test
        @DisplayName("Changing the email requires the current password and a free, valid address")
        void emailChange() throws Exception {
            Client c = createClient("client@venus.test");
            createClient("taken@venus.test");
            patchData(c, Map.of("email", "new@venus.test")).andExpect(status().isBadRequest());
            patchData(c, Map.of("email", "TAKEN@venus.test", "oldPassword", PASSWORD)).andExpect(status().isConflict());
            patchData(c, Map.of("email", "not-an-email", "oldPassword", PASSWORD)).andExpect(status().isBadRequest());
            assertThat(reload(c).getEmail().getValue()).isEqualTo("client@venus.test");
        }

        @Test
        @DisplayName("A failed check leaves the account untouched (no partial update)")
        void noPartialUpdate() throws Exception {
            Client c = createClient("client@venus.test");
            patchData(c, Map.of("username", "Renamed", "password", "short", "oldPassword", PASSWORD))
                    .andExpect(status().isBadRequest());
            assertThat(reload(c).getUsername()).isEqualTo("client");
        }

        @Test
        @DisplayName("Usernames are limited in size")
        void usernameSize() throws Exception {
            Client c = createClient("client@venus.test");
            patchData(c, Map.of("username", "x".repeat(51))).andExpect(status().isBadRequest());
        }
    }

    // ── Admin privileges ─────────────────────────────────────────────────────

    @Nested
    class AdminPrivileges {

        private org.springframework.test.web.servlet.ResultActions setStatus(Admin actor, VenusUser target, String body) throws Exception {
            return mvc.perform(post("/admin/profiles/" + target.getId() + "/status").header("Authorization", bearer(actor))
                    .contentType(MediaType.APPLICATION_JSON).content(body));
        }

        @Test
        @DisplayName("An admin can ban a worker, which ends the worker's sessions")
        void adminBansWorker() throws Exception {
            Admin admin = createAdmin("admin@venus.test");
            Worker w = createVisibleWorker("luna@venus.test");
            String workerAuth = bearer(w);
            setStatus(admin, w, "{\"banned\":true}").andExpect(status().isOk());
            mvc.perform(get("/account/worker/me").header("Authorization", workerAuth)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("An admin cannot ban, lock or delete another admin")
        void adminCannotTouchAdmin() throws Exception {
            Admin admin = createAdmin("admin@venus.test");
            Admin other = createAdmin("other@venus.test");
            setStatus(admin, other, "{\"banned\":true}").andExpect(status().isForbidden());
            setStatus(admin, other, "{\"locked\":true}").andExpect(status().isForbidden());
            mvc.perform(delete("/admin/users/" + other.getId()).header("Authorization", bearer(admin)))
                    .andExpect(status().isForbidden());
            assertThat(reload(other).isBanned()).isFalse();
            assertThat(userRepository.findById(other.getId())).isPresent();
        }

        @Test
        @DisplayName("Nobody can ban or delete a super admin")
        void superAdminProtected() throws Exception {
            Admin admin = createAdmin("admin@venus.test");
            SuperAdmin root = createSuperAdmin("root@venus.test");
            SuperAdmin root2 = createSuperAdmin("root2@venus.test");
            setStatus(admin, root, "{\"banned\":true}").andExpect(status().isForbidden());
            setStatus(root2, root, "{\"banned\":true}").andExpect(status().isForbidden());
            mvc.perform(delete("/admin/users/" + root.getId()).header("Authorization", bearer(admin)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("A super admin can moderate admins")
        void superAdminModeratesAdmins() throws Exception {
            SuperAdmin root = createSuperAdmin("root@venus.test");
            Admin admin = createAdmin("admin@venus.test");
            setStatus(root, admin, "{\"banned\":true}").andExpect(status().isOk());
            assertThat(reload(admin).isBanned()).isTrue();
        }

        @Test
        @DisplayName("An admin cannot change their own status")
        void noSelfStatus() throws Exception {
            Admin admin = createAdmin("admin@venus.test");
            setStatus(admin, admin, "{\"banned\":false,\"locked\":false}").andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Credit days are bounded")
        void creditDaysBounded() throws Exception {
            Admin admin = createAdmin("admin@venus.test");
            Worker w = createWorker("w@venus.test");
            for (int days : new int[]{-1, 100_000}) {
                mvc.perform(post("/admin/profiles/update-days").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("workerId", w.getId().toString(), "newDaysValue", days, "reason", "test"))))
                        .andExpect(status().isBadRequest());
            }
        }

        @Test
        @DisplayName("Region names are validated and loops in the hierarchy are refused")
        void regions() throws Exception {
            Admin admin = createAdmin("admin@venus.test");
            String auth = bearer(admin);
            mvc.perform(post("/admin/region").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\"}")).andExpect(status().isBadRequest());
            mvc.perform(post("/admin/region").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Orphan\",\"parentId\":424242}")).andExpect(status().isNotFound());

            GeographicZone parent = zoneRepository.save(new GeographicZone("Parent", null));
            GeographicZone child = zoneRepository.save(new GeographicZone("Child", parent));

            mvc.perform(post("/admin/region").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("id", parent.getId(), "name", "Parent", "parentId", child.getId()))))
                    .andExpect(status().isBadRequest());
            mvc.perform(post("/admin/region").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("id", parent.getId(), "name", "Parent", "parentId", parent.getId()))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Service names are validated and must stay unique")
        void services() throws Exception {
            Admin admin = createAdmin("admin@venus.test");
            String auth = bearer(admin);
            mvc.perform(post("/admin/service").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"  \"}")).andExpect(status().isBadRequest());
            mvc.perform(post("/admin/service").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Massage\"}")).andExpect(status().isOk());
            mvc.perform(post("/admin/service").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Massage\"}")).andExpect(status().isConflict());

            Service other = new Service();
            other.setName("Dinner");
            other = serviceRepository.save(other);
            mvc.perform(post("/admin/service").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of("id", other.getId(), "name", "Massage")))).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("An admin invitation cannot reuse an existing account's email")
        void inviteExistingEmail() throws Exception {
            Admin admin = createAdmin("admin@venus.test");
            createClient("client@venus.test");
            mvc.perform(post("/admin/invite").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"CLIENT@venus.test\"}")).andExpect(status().isConflict());
            mvc.perform(post("/admin/invite").header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"nope\"}")).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Admins cannot blank out a comment")
        void editCommentValidated() throws Exception {
            Admin admin = createAdmin("admin@venus.test");
            Comment comment = commentRepository.save(new Comment(createClient("c@venus.test"), createVisibleWorker("w@venus.test"), "Hi"));
            mvc.perform(patch("/admin/comments/" + comment.getId()).header("Authorization", bearer(admin))
                    .contentType(MediaType.TEXT_PLAIN).content("   ")).andExpect(status().isBadRequest());
        }
    }
}
