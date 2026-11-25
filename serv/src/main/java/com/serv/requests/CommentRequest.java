package com.serv.requests;

import com.serv.common.InputLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CommentRequest(
        @NotBlank @Size(max = InputLimits.COMMENT_MAX) String content,
        @NotNull UUID workerId
) {
}
