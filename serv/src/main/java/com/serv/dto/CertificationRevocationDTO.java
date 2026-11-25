package com.serv.dto;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CertificationRevocationDTO(
        @NotNull UUID workerId,
        @Size(max = InputLimits.REASON_MAX) String reason) { }
