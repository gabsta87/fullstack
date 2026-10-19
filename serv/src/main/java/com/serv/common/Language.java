package com.serv.common;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum Language {
    EN("English"),
    FR("Français"),
    IT("Italiano"),
    DE("Deutsch"),
    ES("Español"),
    PT("Português"),
    RU("Русский"),
    UA("Українська"),
    AR("العربية");

    private final String displayName;

    Language(String displayName) {
        this.displayName = displayName;
    }

    public static Optional<Language> fromRepositoryOrString(String code) {
        if (code == null || code.isBlank()) return Optional.empty();

        return Arrays.stream(Language.values())
                .filter(lang -> lang.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}