package com.serv.database.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
public abstract class Media {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @Column(columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    protected UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    protected Worker worker;

    @Column(name = "sort_order")
    protected Integer sortOrder = 0;

    @Column(name = "file_size")
    protected Long fileSize = 0L;

    @Column(name = "url", length = 512, nullable = false)
    protected String url;

    @Column(name = "main_thumb_url", length = 512)
    private String mainThumbUrl;
}
