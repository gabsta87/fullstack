package com.serv.requests;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record Register(
        @NotBlank @Size(max = InputLimits.EMAIL_MAX) String email,
        @NotNull @Size(min = InputLimits.PASSWORD_MIN, max = InputLimits.PASSWORD_MAX_BYTES) String password
) { }
