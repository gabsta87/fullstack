package com.serv.dto;

import com.serv.common.SettingKey;

/** A setting with its current value and the range the admins may choose from. */
public record SettingDTO(String key, int value, int defaultValue, int min, int max) {

    public static SettingDTO of(SettingKey key, int value) {
        return new SettingDTO(key.name(), value, key.defaultValue(), key.min(), key.max());
    }
}
