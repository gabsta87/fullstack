package com.serv.requests;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.Size;

public record AccountData(
        @Size(max = InputLimits.USERNAME_MAX) String username,
        @Size(max = InputLimits.EMAIL_MAX) String email,
        @Size(min = InputLimits.PASSWORD_MIN, max = InputLimits.PASSWORD_MAX_BYTES) String password,
        @Size(max = InputLimits.PASSWORD_INPUT_MAX) String oldPassword
) {}
