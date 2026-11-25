package com.serv.requests.admin;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateLegalText(
        @NotBlank @Size(max = InputLimits.NAME_MAX) String key,
        @NotNull @Size(max = InputLimits.LEGAL_CONTENT_MAX) String content) {
}
