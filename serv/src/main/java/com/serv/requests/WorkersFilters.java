package com.serv.requests;

import java.util.List;

public record WorkersFilters(
        Integer page,
        String zoneId,
        String username,
        String gender,
        String bodyType,
        String eyeColor,
        String hairColor,
        Integer minAge,
        Integer maxAge,
        List<String> languages,
        List<String> services
) {
    public int getPageOrZero() {
        return page != null ? page : 0;
    }
}
