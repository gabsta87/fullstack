package com.serv.database.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.serv.common.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("WORKER")
@Table(name = TablesNames.WORKERS, indexes = {
        @Index(name = "idx_worker_available_position", columnList = "available, galleryPositionPriority")
})
public class Worker extends VenusUser {

    @ToString.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_photo_id")
    private Photo mainPhoto;

    @ToString.Exclude
    @BatchSize(size = 20)
    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Photo> photos = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Comment> comments = new ArrayList<>();

    @ToString.Exclude
    @BatchSize(size = 20)
    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Video> videos = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "workers_services", // Table de jointure
            joinColumns = @JoinColumn(name = "worker_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<Service> services = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", length = 16)
    private BodyType bodyType;
    @Enumerated(EnumType.STRING)
    @Column(name = "eye_color")
    private EyeColor eyeColor;
    @Enumerated(EnumType.STRING)
    @Column(name = "hair_color")
    private HairColor hairColor;
    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    private String phone;

    // Use to sort the photos in the gallery. Higher comes first.
    private int galleryPositionPriority;

    // disabled by admins
    private boolean disabled;
    // hidden by the worker himself
    private boolean hidden;
    private boolean banned;
    // No more days available for the worker
    private boolean expired;
    // Available for work by the worker himself.
    private boolean available;
    // Certified by admins when the requested photo has been confirmed
    private boolean certified;
    // Has the worker been active today? set by system
    private boolean hasBeenActiveToday;

    // Instant stored as UTC timestamp
    @Column(name = "last_refreshed")
    private Instant lastRefreshed;

    // Date stored as DATE only (no time component)
    @Temporal(TemporalType.DATE)
    @Column(name = "birthdate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "GMT+2")
    private Date birthdate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "remaining_days_credit")
    private Integer remainingDaysCredit;

    @Column(name = "verification_code")
    private String verificationCode; // Le mot/nombre secret généré par le site pour sa photo

    @Column(name = "certification_status")
    private String certificationStatus; // NOT_REQUESTED, PENDING_APPROVAL, CERTIFIED, REJECTED

    @Column(name = "certified_at")
    private LocalDateTime certifiedAt;

    @Column(name = "certification_expires_at")
    private LocalDateTime certificationExpiresAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "workers_languages", // Nom de ta nouvelle table de jointure
            joinColumns = @JoinColumn(name = "worker_id")
    )
    private Collection<WorkerLanguage> spokenLanguages = new ArrayList<>();

    public Worker(Email email, String password) {
        super(email, password);
        this.role = UserRole.WORKER;
    }

    public Worker(String username, Email email, String password) {
        this(email,password);
        this.username = username;
    }

    public Worker() {
        this.role = UserRole.WORKER;
    }

    public void parseBirthdate(String birthdate) throws ParseException {
        if (birthdate == null || birthdate.isBlank()) {
            this.birthdate = null;
            return;
        }

        if (birthdate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            this.birthdate = new SimpleDateFormat("yyyy-MM-dd").parse(birthdate);
        } else {
            this.birthdate = new SimpleDateFormat("dd/MM/yyyy").parse(birthdate);
        }
    }

    public void addPhoto(Photo photo) {
        this.photos.add(photo);
    }

    public void removePhoto(Photo photo) {
        this.photos.remove(photo);
    }

    public void addSpokenLanguage(WorkerLanguage language) {this.spokenLanguages.add(language);}

    public void removeSpokenLanguage(WorkerLanguage language) {this.spokenLanguages.remove(language);}

    public void setActive(boolean active) {
        this.available = active;
        this.hasBeenActiveToday = active;
    }
}
