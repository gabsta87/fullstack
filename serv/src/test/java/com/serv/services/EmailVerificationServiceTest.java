package com.serv.services;

import com.serv.database.entities.Client;
import com.serv.database.entities.Email;
import com.serv.database.entities.VerificationToken;
import com.serv.database.repositories.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.serv.support.TestEmailTemplates;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private MailService mailService;

    @Spy
    private EmailTemplates emailTemplates = TestEmailTemplates.create();

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        // Inject the frontend URL: @Value is not resolved outside a Spring Boot context
        ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", "http://localhost:4200");
    }

    @Test
    @DisplayName("createTokenAndSendEmail - cleans up, saves and sends the verification email")
    void createTokenAndSendEmail_Success() {
        Client client = new Client(new Email("client@venus.com"), "Password123!");

        when(verificationTokenRepository.save(any(VerificationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String verificationUrl = emailVerificationService.createTokenAndSendEmail(client);

        assertThat(verificationUrl).contains("http://localhost:4200/verify-email?token=");

        // Check the interactions
        verify(verificationTokenRepository, times(1)).deleteAllByUserId(client.getId());
        verify(verificationTokenRepository, times(1)).flush();
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        verify(mailService, times(1)).sendHtmlMessage(eq(client.getEmail()), eq("Confirm your email address"), anyString());
    }
}