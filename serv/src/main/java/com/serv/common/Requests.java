package com.serv.common;

import com.serv.dto.VenusUserDTO;

import java.util.List;

public class Requests {

    public record RegisterRequest(String email, String password) { }

    public record ForgotPasswordRequest (String email) { }

    public record LoginRequest(String email, String password) { }
    public record LoginResponse(String token, VenusUserDTO user) { }

    public record WorkerProfileUpdateRequest(
            String description,
            Integer geographicZoneId,
            String eyeColor,
            String hairColor,
            String phone,
            String bodyType,
            String mainPhotoId,
            List<String> services,
            String birthdate
    ) {}

    public record AccountDataRequest(
            String username,
            String email,
            String password
    ) {}

    public record WorkerSearchRequest(
            Integer page,
            String zoneId,
            String username,
            String bodyType,
            List<String> services,
            String eyeColor,
            String hairColor
    ) {
        public int getPageOrZero() {
            return page != null ? page : 0;
        }
    }
}
