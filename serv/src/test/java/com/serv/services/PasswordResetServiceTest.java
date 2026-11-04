package com.serv.services;

import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.PasswordResetToken;
import com.serv.database.repositories.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:4200");
    }

    @Test
    @DisplayName("createTokenAndSendEmail - Nettoie, sauvegarde et envoie l'e-mail de réinitialisation")
    void createTokenAndSendEmail_Success() {
        Client client = new Client(new Email("client@venus.com"), "Password123!");

        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String resetUrl = passwordResetService.createTokenAndSendEmail(client, false);

        assertThat(resetUrl).contains("http://localhost:4200/reset-password?resetToken=");

        verify(passwordResetTokenRepository, times(1)).deleteAllByUserId(client.getId());
        verify(passwordResetTokenRepository, times(1)).flush();
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(mailService, times(1)).sendHtmlMessage(eq(client.getEmail()), eq("Réinitialisation de votre mot de passe"), anyString());
    }
}