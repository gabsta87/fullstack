package com.serv.database.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "global_configurations")
@Data
public class GlobalConfiguration {
    @Id
    @Column(name = "config_key")
    private String configKey; // ex: "DAILY_PRICE_CENTS", "LEGAL_TERMS_FR", "LEGAL_CONTRACT_EN"

    @Column(name = "config_value", length = 10000) // "10000" pour stocker de longs textes
    private String configValue;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}