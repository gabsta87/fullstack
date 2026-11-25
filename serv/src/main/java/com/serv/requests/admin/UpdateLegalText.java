package com.serv.requests.admin;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateLegalText(
        @NotBlank @Pattern(regexp = InputLimits.LEGAL_KEY_PATTERN) String key,
        @NotNull @Pattern(regexp = InputLimits.SITE_LANGUAGE_PATTERN) String language,
        @NotBlank @Size(max = InputLimits.LEGAL_CONTENT_MAX) String content) {
}
