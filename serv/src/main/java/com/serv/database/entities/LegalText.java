package com.serv.database.entities;

import com.serv.common.InputLimits;
import com.serv.common.TableNames;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A version of a legal text in one language. Rows are never updated: each change adds a new
 * version, so the text a visitor accepted at a given date can always be shown (legal proof).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = TableNames.LEGAL_TEXTS, indexes = {
        @Index(name = "idx_legal_name_language_version", columnList = "name, language, version")
})
public class LegalText {

    /** Notice shown on the first visit: the site is reserved for adults. */
    public static final String ADULT_NOTICE = "ADULT_NOTICE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    /** Site language code: fr, en, de, es, pt, it, ru, ua, ar. */
    @Column(length = 5)
    private String language;
    /** Plain text (never rendered as HTML). Column "body": the former varchar "content" was too short. */
    @Lob
    @Column(name = "body", length = InputLimits.LEGAL_CONTENT_MAX)
    private String content;
    private LocalDateTime lastUpdate;
    private int version;
    @ManyToOne
    @JoinColumn(name = "author_id")
    private Admin author;
}
