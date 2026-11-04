package com.serv.services;

import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.VerificationToken;
import com.serv.database.repositories.VerificationTokenRepository;
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
class EmailVerificationServiceTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        // Injecte la valeur de l'URL du frontend puisque @Value ne s'initialise pas hors contexte Spring Boot
        ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", "http://localhost:4200");
    }

    @Test
    @DisplayName("createTokenAndSendEmail - Nettoie, sauvegarde et envoie l'e-mail de vérification")
    void createTokenAndSendEmail_Success() {
        Client client = new Client(new Email("client@venus.com"), "Password123!");

        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String verificationUrl = emailVerificationService.createTokenAndSendEmail(client);

        assertThat(verificationUrl).contains("http://localhost:4200/verify-email?token=");

        // Vérification des interactions
        verify(verificationTokenRepository, times(1)).deleteAllByUserId(client.getId());
        verify(verificationTokenRepository, times(1)).flush();
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        verify(mailService, times(1)).sendHtmlMessage(eq(client.getEmail()), eq("Validation de votre adresse e-mail"), anyString());
    }
}