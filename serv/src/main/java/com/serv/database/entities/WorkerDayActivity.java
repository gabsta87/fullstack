package com.serv.database.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "worker_day_activities")
@Data
@NoArgsConstructor
public class WorkerDayActivity {

    @EmbeddedId
    private WorkerDayActivityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("workerId")
    @JoinColumn(name = "worker_id")
    private Worker worker;

    private boolean isAvailable;
    private boolean isBanned;
    private boolean isExpired;
    private boolean isHidden;
    private boolean isInvalid;
    private boolean isLocked;

    public WorkerDayActivity(Worker worker, LocalDate date) {
        this.worker = worker;
        this.id = new WorkerDayActivityId(worker.getId(), date);

        this.isAvailable = worker.isAvailable();
        this.isBanned = worker.isBanned();
        this.isExpired = worker.isExpired();
        this.isHidden = worker.isHidden();
        this.isInvalid = worker.isInvalid();
        this.isLocked = worker.isLocked();
    }

    public boolean isPayDue(){
        return this.isAvailable && !this.isExpired && !this.isHidden && !this.isInvalid;
    }
}