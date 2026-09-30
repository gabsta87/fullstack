package com.serv.database.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
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
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(name = "verification_code", nullable = false, length = 10)
    private String verificationCode;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public CertificationRequest(Worker worker) {
        this.worker = worker;
        this.verificationCode = worker.getVerificationCode();
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }
}