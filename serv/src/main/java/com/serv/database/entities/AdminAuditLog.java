package com.serv.database.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
@Data
public class AdminAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "action_type", nullable = false)
    private String actionType; // ex: "UPDATE_DAYS", "CERTIFY_PROFILE", "TOGGLE_STATUS"

    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private VenusUser userTarget;

    @ManyToOne
    @JoinColumn(name = "target_geographic_zone_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private GeographicZone regionTarget;

    @ManyToOne
    @JoinColumn(name = "target_service_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Service serviceTarget;

    @ManyToOne
    @JoinColumn(name = "target_comment_id")
    private Comment commentTarget;

    @ManyToOne
    @JoinColumn(name = "target_legal_text_id")
    private LegalText legalTextTarget;

    @Column(columnDefinition = "json", name = "target_snapshot")
    private String targetSnapshot;

    @Column(name = "details", length = 1000)
    private String details; // ex: "Passage du crédit de 5 jours à 30 jours"

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public void setTarget(VenusUser userTarget){
        this.userTarget = userTarget;
    }

    public void setTarget(GeographicZone regionTarget){
        this.regionTarget = regionTarget;
    }

    public void setTarget(Service serviceTarget){
        this.serviceTarget = serviceTarget;
    }
    public void setTarget(Comment commentTarget){
        this.commentTarget = commentTarget;
    }

    public void setTarget(LegalText legalTextTarget){
        this.legalTextTarget = legalTextTarget;
    }

    public void setTarget(String targetSnapshot){
        this.targetSnapshot = targetSnapshot;
    }
}