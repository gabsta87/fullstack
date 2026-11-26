package com.serv.database.entities;

import com.serv.common.TableNames;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * "Notify me when back": a visitor waits for an unavailable profile to become available again.
 * One email is sent at the next return, then the alert is deleted. Deleting either account deletes it.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = TableNames.AVAILABILITY_ALERTS,
        uniqueConstraints = @UniqueConstraint(name = "uk_alert_client_worker", columnNames = {"client_id", "worker_id"}),
        indexes = @Index(name = "idx_alert_worker", columnList = "worker_id"))
public class AvailabilityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Worker worker;

    /** Site language when the alert was set (BCP 47 tag): the email is sent later, outside any request. */
    @Column(nullable = false, length = 10)
    private String language;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AvailabilityAlert(Client client, Worker worker, String language) {
        this.client = client;
        this.worker = worker;
        this.language = language;
    }
}
