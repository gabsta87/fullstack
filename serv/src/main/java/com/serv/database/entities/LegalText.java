package com.serv.database.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class LegalText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String content;
    private LocalDateTime lastUpdate;
    private int version;
    @ManyToOne
    @JoinColumn(name = "author_id")
    private Admin author;
}
