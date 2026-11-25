package com.serv.requests.admin;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateService(
        Integer id,
        @NotBlank @Size(max = InputLimits.NAME_MAX) String name,
        @Size(max = InputLimits.DESCRIPTION_MAX) String description) {
}
