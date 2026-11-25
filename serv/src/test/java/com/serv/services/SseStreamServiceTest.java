package com.serv.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SseStreamServiceTest {

    /** Records the events instead of writing them to an HTTP response. */
    static class RecordingEmitter extends SseEmitter {
        final List<Object> sent = new ArrayList<>();
        boolean completed;

        @Override
        public void send(SseEventBuilder builder) {
            sent.add(builder.build());
        }

        @Override
        public void complete() {
            completed = true;
        }
    }

    private final SseStreamService service = new SseStreamService();

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static void commit() {
        List<TransactionSynchronization> hooks = TransactionSynchronizationManager.getSynchronizations();
        TransactionSynchronizationManager.clearSynchronization();
        hooks.forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    @DisplayName("Admin events are sent only after the commit: other admins must not reload stale data")
    void adminEventsAfterCommit() {
        RecordingEmitter admin = new RecordingEmitter();
        service.createAdminStream(UUID.randomUUID(), admin);

        TransactionSynchronizationManager.initSynchronization();
        service.broadcastAdminEvent("USERS_UPDATED", "x");
        assertThat(admin.sent).isEmpty();

        commit();
        assertThat(admin.sent).hasSize(1);
    }

    @Test
    @DisplayName("Outside a transaction, admin events are sent right away")
    void adminEventsWithoutTransaction() {
        RecordingEmitter admin = new RecordingEmitter();
        service.createAdminStream(UUID.randomUUID(), admin);
        service.broadcastAdminEvent("SERVICES_UPDATED", 1);
        assertThat(admin.sent).hasSize(1);
    }

    @Test
    @DisplayName("Ending a session notifies only that user's pages and closes their streams")
    void endSession() {
        UUID banned = UUID.randomUUID();
        RecordingEmitter bannedTab = new RecordingEmitter();
        RecordingEmitter otherUser = new RecordingEmitter();
        service.createStream(banned, bannedTab);
        service.createStream(UUID.randomUUID(), otherUser);
        int initEvents = bannedTab.sent.size();

        service.endSession(banned);

        assertThat(bannedTab.sent).hasSize(initEvents + 1);
        assertThat(bannedTab.completed).isTrue();
        assertThat(service.activeStreamCount(banned)).isZero();
        assertThat(otherUser.completed).isFalse();
        assertThat(otherUser.sent).hasSize(initEvents);
    }
}
