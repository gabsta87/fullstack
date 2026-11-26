package com.serv.services;

import com.serv.support.TestEmailTemplates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplatesTest {

    private final EmailTemplates templates = TestEmailTemplates.create();

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Emails are in English when the request language has no translation")
    void englishByDefault() {
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        EmailTemplates.RenderedEmail email = templates.render(EmailTemplates.Kind.RESET_PASSWORD, "https://venus.test/reset?t=1");
        assertThat(email.subject()).isEqualTo("Reset your password");
        assertThat(email.html()).contains("Hello,").contains("https://venus.test/reset?t=1");
    }

    @Test
    @DisplayName("Emails follow the site language sent by the front (Accept-Language)")
    void followsRequestLanguage() {
        LocaleContextHolder.setLocale(Locale.FRENCH);
        assertThat(templates.render(EmailTemplates.Kind.VERIFY_EMAIL, "u").subject()).isEqualTo("Validation de votre adresse e-mail");
        LocaleContextHolder.setLocale(Locale.forLanguageTag("uk"));
        assertThat(templates.render(EmailTemplates.Kind.ADMIN_INVITATION, "u").subject()).isEqualTo("Активація облікового запису адміністратора");
        LocaleContextHolder.setLocale(Locale.forLanguageTag("de-CH"));
        assertThat(templates.render(EmailTemplates.Kind.RESET_PASSWORD, "u").subject()).isEqualTo("Passwort zurücksetzen");
    }

    @Test
    @DisplayName("Texts and the link are HTML-escaped")
    void escapesHtml() {
        LocaleContextHolder.setLocale(Locale.FRENCH);
        String html = templates.render(EmailTemplates.Kind.RESET_PASSWORD, "https://x/?a='><script>").html();
        assertThat(html).doesNotContain("<script>").doesNotContain("'><");
        assertThat(html).contains("L&#39;équipe Venus");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Every email exists in every site language, with no missing or raw key")
    void everyKindInEveryLanguage() {
        for (String tag : new String[]{"en", "fr", "de", "es", "pt", "it", "ru", "uk", "ar"}) {
            for (EmailTemplates.Kind kind : EmailTemplates.Kind.values()) {
                EmailTemplates.RenderedEmail email = templates.render(kind, "https://venus.test/x", Locale.forLanguageTag(tag), "Luna");
                assertThat(email.subject()).as(tag + " " + kind).isNotBlank().doesNotContain("email.").doesNotContain("{0}");
                assertThat(email.html()).as(tag + " " + kind).doesNotContain("email.").doesNotContain("{0}");
            }
        }
    }

    @Test
    @org.junit.jupiter.api.DisplayName("The profile name in the back-online email is escaped")
    void backOnlineNameEscaped() {
        EmailTemplates.RenderedEmail email = templates.render(EmailTemplates.Kind.BACK_ONLINE, "u", Locale.FRENCH, "<b>Luna</b>");
        assertThat(email.html()).contains("&lt;b&gt;Luna&lt;/b&gt;").doesNotContain("<b>Luna");
        assertThat(email.subject()).isEqualTo("<b>Luna</b> est de retour sur Venus"); // plain-text header
    }
}
