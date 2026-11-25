package com.serv.requests.admin;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRegion(
        Integer id,
        @NotBlank @Size(max = InputLimits.NAME_MAX) String name,
        Integer parentId) { }
