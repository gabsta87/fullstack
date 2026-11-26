package com.serv.requests.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** New values, by setting name ({@code SettingKey}). Names and ranges are checked by {@code SettingsService}. */
public record UpdateSettings(@NotNull @NotEmpty @Size(max = 20) Map<String, Integer> values) {
}
