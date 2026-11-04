package com.serv.services;

import com.serv.database.entities.Worker;
import com.serv.database.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCleanupServiceTest {

    @Mock private MediaStorageService storageService;
    @Mock private CertificationRequestRepository certificationRequestRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private AdminAuditLogRepository adminAuditLogRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserCleanupService userCleanupService;

    @Test
    @DisplayName("deleteUserCompletely - Supprime un worker, ses fichiers et ses dépendances")
    void deleteUserCompletely_WorkerSuccess() throws IOException {
        Worker worker = new Worker();
        worker.setId(UUID.randomUUID());
        worker.setUsername("WorkerToDelete");

        doNothing().when(storageService).deleteAllForUser(worker.getId());
        when(userRepository.save(any(Worker.class))).thenReturn(worker);
        doNothing().when(certificationRequestRepository).deleteByWorkerId(worker.getId());
        doNothing().when(adminAuditLogRepository).nullifyUserTarget(worker.getId());
        doNothing().when(passwordResetTokenRepository).deleteAllByUserId(worker.getId());
        doNothing().when(userRepository).delete(worker);

        userCleanupService.deleteUserCompletely(worker);

        verify(storageService, times(1)).deleteAllForUser(worker.getId());
        verify(certificationRequestRepository, times(1)).deleteByWorkerId(worker.getId());
        verify(adminAuditLogRepository, times(1)).nullifyUserTarget(worker.getId());
        verify(passwordResetTokenRepository, times(1)).deleteAllByUserId(worker.getId());
        verify(userRepository, times(1)).delete(worker);
    }
}