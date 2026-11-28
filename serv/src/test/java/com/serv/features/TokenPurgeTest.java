package com.serv.features;

import com.serv.database.entities.Client;
import com.serv.database.entities.PasswordResetToken;
import com.serv.database.repositories.PasswordResetTokenRepository;
import com.serv.services.EmailVerificationService;
import com.serv.services.MailService;
import com.serv.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TokenPurgeTest extends IntegrationTest {

    @Autowired MailService mailService;
    @Autowired EmailVerificationService emailVerificationService;
    @Autowired PasswordResetTokenRepository resetTokens;

    @Test
    @DisplayName("The nightly purge removes expired password reset links only (it failed every night: wrong date type)")
    void purgeExpiredResetLinks() {
        Client c = createClient("client@venus.test");
        PasswordResetToken expired = new PasswordResetToken(UUID.randomUUID(), c);
        expired.setExpiryDate(Instant.now().minusSeconds(60));
        PasswordResetToken valid = new PasswordResetToken(UUID.randomUUID(), createClient("other@venus.test")); // one link per account
        resetTokens.save(expired);
        resetTokens.save(valid);

        mailService.purgeExpiredTokens();

        assertThat(resetTokens.findAll()).extracting(PasswordResetToken::getId).containsExactly(valid.getId());
        assertThatCode(emailVerificationService::purgeExpiredVerificationTokens).doesNotThrowAnyException();
    }
}
