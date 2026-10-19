package com.serv.requests;

import com.serv.dto.SpokenLanguageDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateWorkerLanguage(
        @Valid
        @NotNull
        List<SpokenLanguageDTO> languages) {
}
