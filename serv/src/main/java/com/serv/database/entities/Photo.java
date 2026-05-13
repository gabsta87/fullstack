package com.serv.database.entities;

import com.serv.common.TablesNames;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Data
@Entity
@Table(name = TablesNames.PHOTOS, indexes = {
        @Index(name = "idx_photo_worker", columnList = "worker_id")
})
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String fileName;
    private String title;
    private String description;

    /** Full-resolution URL served by Nginx */
    @Column(name = "original_url", length = 512)
    private String originalUrl;

    /** 600×800 portrait crop — shown on gallery cards */
    @Column(name = "main_thumb_url", length = 512)
    private String mainThumbUrl;

    /** 400×300 landscape crop — used in hover preview carousel */
    @Column(name = "preview_thumb_url", length = 512)
    private String previewThumbUrl;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private Worker worker;

}
