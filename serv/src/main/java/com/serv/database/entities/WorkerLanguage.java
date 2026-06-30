package com.serv.database.entities;

import com.serv.common.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class WorkerLanguage {

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private Language language;

    // Niveau de maîtrise (1 à 3)
    @Column(name = "mastery_level", nullable = false)
    private int masteryLevel;
}