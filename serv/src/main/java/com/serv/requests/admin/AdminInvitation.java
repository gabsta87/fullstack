package com.serv.requests.admin;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminInvitation(@NotBlank @Size(max = InputLimits.EMAIL_MAX) String email) {
}
