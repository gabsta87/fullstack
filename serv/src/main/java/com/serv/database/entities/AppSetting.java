package com.serv.database.entities;

import com.serv.common.SettingKey;
import com.serv.common.TableNames;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** One admin setting. A missing row means the default value of its {@link SettingKey}. */
@Data
@NoArgsConstructor
@Entity
@Table(name = TableNames.APP_SETTINGS)
public class AppSetting {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private SettingKey name;

    @Column(name = "setting_value", nullable = false)
    private int value;

    private LocalDateTime updatedAt;

    /** Admin who made the last change (plain id: the audit log keeps the history). */
    private UUID updatedBy;

    public AppSetting(SettingKey name) {
        this.name = name;
    }
}
