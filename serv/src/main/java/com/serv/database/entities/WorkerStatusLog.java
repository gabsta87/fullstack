package com.serv.database.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "worker_status_logs")
@Data
@NoArgsConstructor
public class WorkerStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    // Qui a provoqué le changement ? ("WORKER", "ADMIN", "SYSTEM")
    @Column(nullable = false)
    private String triggeredBy;

    // Capture de l'état exact
    private boolean isAvailable;
    private boolean isHidden;
    private boolean isBanned;
    private boolean isLocked;
    private boolean isExpired;
    private boolean isInvalid;

    public WorkerStatusLog(Worker worker, String triggeredBy) {
        this.worker = worker;
        this.triggeredBy = triggeredBy;
        this.isAvailable = worker.isAvailable();
        this.isHidden = worker.isHidden();
        this.isBanned = worker.isBanned();
        this.isLocked = worker.isLocked();
        this.isExpired = worker.isExpired();
        this.isInvalid = worker.isInvalid();
    }
}