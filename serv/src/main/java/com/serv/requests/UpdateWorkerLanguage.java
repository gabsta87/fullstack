package com.serv.requests;

import com.serv.common.InputLimits;
import com.serv.dto.SpokenLanguageDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateWorkerLanguage(
        @NotNull
        @Size(max = InputLimits.LIST_MAX)
        List<@Valid @NotNull SpokenLanguageDTO> languages) {
}
