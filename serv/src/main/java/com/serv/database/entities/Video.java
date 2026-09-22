package com.serv.database.entities;

import com.serv.common.TablesNames;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = TablesNames.VIDEOS, indexes = {
        @Index(name = "idx_video_worker", columnList = "worker_id")
})
public class Video extends Media{

    private Integer duration;
}