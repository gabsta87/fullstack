package com.serv.database.entities;

import com.serv.common.TablesNames;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = TablesNames.PHOTOS, indexes = {
        @Index(name = "idx_photo_worker", columnList = "worker_id")
})
public class Photo extends Media{

    /** 400×300 landscape crop — used in hover preview carousel */
    @Column(name = "preview_thumb_url", length = 512)
    private String previewThumbUrl;

}
