package com.serv.database.entities;

import com.serv.common.TableNames;
import com.serv.common.VideoStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = TableNames.VIDEOS, indexes = {
        @Index(name = "idx_video_worker", columnList = "worker_id")
})
public class Video extends Media{

    /** Seconds, known once processed. */
    private Integer duration;

    /** Null for videos uploaded before processing existed: they count as ready. */
    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private VideoStatus status;

    /** Why processing failed (an English message key for the front, see the {@code video_*} messages). */
    @Column(length = 40)
    private String failureReason;

    public boolean isReady() {
        return status == null || status == VideoStatus.READY;
    }
}