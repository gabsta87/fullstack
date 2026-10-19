package com.serv.dto;

import com.serv.common.Language;
import com.serv.database.entities.WorkerLanguage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SpokenLanguageDTO(
        @NotNull(message = "Language required")
        Language language,

        @NotNull(message = "Mastery level required")
        @Min(value = 1, message = "Minimum is 1")
        @Max(value = 4, message = "Maximum is 4")
        Integer level
) {
    public static SpokenLanguageDTO from(WorkerLanguage arg){
        return new SpokenLanguageDTO(arg.getLanguage(), arg.getMasteryLevel());
    }
}
