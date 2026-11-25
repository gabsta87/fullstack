package com.serv.requests.admin;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateWorkerDays(
        @NotNull UUID workerId,
        @Min(0) @Max(InputLimits.MAX_CREDIT_DAYS) int newDaysValue,
        @Size(max = InputLimits.REASON_MAX) String reason) {
}
