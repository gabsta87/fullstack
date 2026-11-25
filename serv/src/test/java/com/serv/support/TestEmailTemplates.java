package com.serv.support;

import com.serv.services.EmailTemplates;
import org.springframework.context.support.ResourceBundleMessageSource;

/** {@link EmailTemplates} on the real {@code messages*.properties}, configured like Spring Boot does. */
public final class TestEmailTemplates {

    private TestEmailTemplates() {}

    public static EmailTemplates create() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return new EmailTemplates(source);
    }
}
