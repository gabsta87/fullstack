package com.serv.requests;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record Report(
        @NotBlank @Size(max = InputLimits.REASON_MAX) String reason,
        @NotNull UUID workerId
) {
}
