package com.serv.database.repositories;

import com.serv.common.SettingKey;
import com.serv.database.entities.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, SettingKey> {
}
