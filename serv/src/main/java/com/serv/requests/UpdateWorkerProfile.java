package com.serv.requests;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Partial update: every null field is left untouched. */
public record UpdateWorkerProfile(
        @Size(max = InputLimits.USERNAME_MAX) String username,
        @Size(max = InputLimits.DESCRIPTION_MAX) String description,
        @Size(max = InputLimits.SHORT_DESCRIPTION_MAX) String shortDescription,
        Integer geographicZoneId,
        @Size(max = 32) String eyeColor,
        @Size(max = 32) String hairColor,
        @Pattern(regexp = InputLimits.PHONE_PATTERN) String phone,
        @Size(max = 32) String bodyType,
        UUID mainPhotoId,
        @Size(max = InputLimits.LIST_MAX) List<@NotBlank @Size(max = InputLimits.NAME_MAX) String> services,
        @Size(max = 10) String birthdate
) {
}
