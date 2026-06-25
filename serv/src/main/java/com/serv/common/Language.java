package com.serv.common;

import java.util.Arrays;
import java.util.Optional;

public enum Language {
    EN,FR,IT,DE,ES;

    public static Optional<Language> fromRepositoryOrString(String code) {
        if (code == null) return Optional.empty();

        return Arrays.stream(Language.values())
                .filter(lang -> lang.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
