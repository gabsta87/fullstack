package com.serv.dto;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CertificationReviewDTO(
        @NotNull Long requestId,
        boolean approved,
        @Size(max = InputLimits.REASON_MAX) String comment) {
}
