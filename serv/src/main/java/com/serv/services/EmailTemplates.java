package com.serv.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.Locale;

/**
 * Builds the transactional emails. Texts live in {@code messages*.properties}; the language is the
 * one of the current request ({@code Accept-Language}, sent by the front from the site language),
 * English by default.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplates {

    public enum Kind { VERIFY_EMAIL, RESET_PASSWORD, ADMIN_INVITATION }

    public record RenderedEmail(String subject, String html) {}

    private final MessageSource messageSource;

    public RenderedEmail render(Kind kind, String url) {
        Locale locale = LocaleContextHolder.getLocale();
        String prefix = switch (kind) {
            case VERIFY_EMAIL -> "email.verify.";
            case RESET_PASSWORD -> "email.reset.";
            case ADMIN_INVITATION -> "email.invite.";
        };
        String safeUrl = HtmlUtils.htmlEscape(url, "UTF-8");
        String html = "<html><head><meta charset=\"UTF-8\"></head><body>"
                + "<p>" + text("email.greeting", locale) + "</p>"
                + "<p>" + text(prefix + "intro", locale) + "</p>"
                + "<p style='margin: 20px 0;'>"
                + "  <a href='" + safeUrl + "' style='background-color: #3880ff; color: white; padding: 10px 20px;"
                + " text-decoration: none; border-radius: 4px; display: inline-block;'>"
                + text(prefix + "button", locale)
                + "  </a>"
                + "</p>"
                + "<p>" + text("email.fallback", locale) + "<br>"
                + "<a href='" + safeUrl + "'>" + safeUrl + "</a></p>"
                + "<p>" + text("email.signature", locale) + "<br>" + text("email.team", locale) + "</p>"
                + "</body></html>";
        return new RenderedEmail(messageSource.getMessage(prefix + "subject", null, locale), html);
    }

    /** Message texts are plain text: escaped before being placed in the HTML. */
    private String text(String key, Locale locale) {
        return HtmlUtils.htmlEscape(messageSource.getMessage(key, null, locale), "UTF-8");
    }
}
