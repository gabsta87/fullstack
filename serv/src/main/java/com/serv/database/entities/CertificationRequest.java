package com.serv.database.entities;

import com.serv.common.CertificationStatus;
import com.serv.database.listeners.CertificationRequestEventListener;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EntityListeners({AuditingEntityListener.class, CertificationRequestEventListener.class})
@Entity
@Table(name = "certification_requests", indexes = {
        @Index(name = "idx_certification_status", columnList = "status")
})
public class CertificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by_admin_id")
    private Admin lockedByAdmin = null;

    private boolean underReview = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(name = "verification_code", nullable = false, length = 10)
    private String verificationCode;

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "photo_id")
    private Photo certificationPhoto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private CertificationStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    private String comment;

    public CertificationRequest(Worker worker) {
        this.worker = worker;
        this.verificationCode = worker.getVerificationCode();
        this.createdAt = LocalDateTime.now();
        this.processedAt = LocalDateTime.now();
        this.status = CertificationStatus.PENDING_APPROVAL;
    }
}