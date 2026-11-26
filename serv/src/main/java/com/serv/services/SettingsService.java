package com.serv.services;

import com.serv.common.SettingKey;
import com.serv.database.entities.AppSetting;
import com.serv.database.repositories.AppSettingRepository;
import com.serv.dto.SettingDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Admin settings, stored in the database (no redeployment needed). Read on demand, without a cache:
 * they are only needed on rare actions (upload, certification), and a primary-key lookup is cheap.
 */
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final AppSettingRepository repository;

    public int get(SettingKey key) {
        return repository.findById(key).map(AppSetting::getValue).orElse(key.defaultValue());
    }

    public long storageLimitBytes() {
        return get(SettingKey.STORAGE_LIMIT_MB) * 1024L * 1024L;
    }

    public List<SettingDTO> all() {
        Map<SettingKey, Integer> stored = new EnumMap<>(SettingKey.class);
        repository.findAll().forEach(s -> stored.put(s.getName(), s.getValue()));
        return Arrays.stream(SettingKey.values())
                .map(k -> SettingDTO.of(k, stored.getOrDefault(k, k.defaultValue())))
                .toList();
    }

    /**
     * Checks and saves new values. Every name must be known and every value in its range; the
     * renewal period must stay shorter than the validity. All or nothing.
     *
     * @return the settings that actually changed, with their previous value
     */
    @Transactional
    public Map<SettingKey, int[]> update(Map<String, Integer> values, UUID adminId) {
        Map<SettingKey, Integer> parsed = new EnumMap<>(SettingKey.class);
        values.forEach((name, value) -> {
            SettingKey key;
            try {
                key = SettingKey.valueOf(name);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown setting.");
            }
            if (value == null || value < key.min() || value > key.max()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        key.name() + " must be between " + key.min() + " and " + key.max() + ".");
            }
            parsed.put(key, value);
        });

        int validity = parsed.getOrDefault(SettingKey.CERTIFICATION_VALIDITY_MONTHS, get(SettingKey.CERTIFICATION_VALIDITY_MONTHS));
        int renewal = parsed.getOrDefault(SettingKey.CERTIFICATION_RENEWAL_MONTHS, get(SettingKey.CERTIFICATION_RENEWAL_MONTHS));
        if (renewal >= validity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The renewal period must be shorter than the certification validity.");
        }

        Map<SettingKey, int[]> changed = new EnumMap<>(SettingKey.class);
        parsed.forEach((key, value) -> {
            AppSetting setting = repository.findById(key).orElseGet(() -> {
                AppSetting created = new AppSetting(key);
                created.setValue(key.defaultValue());
                return created;
            });
            if (setting.getValue() == value && setting.getUpdatedAt() != null) return;
            int previous = setting.getValue();
            setting.setValue(value);
            setting.setUpdatedAt(LocalDateTime.now());
            setting.setUpdatedBy(adminId);
            repository.save(setting);
            if (previous != value) changed.put(key, new int[]{previous, value});
        });
        return changed;
    }
}
