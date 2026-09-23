package com.serv.database.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.serv.common.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Data
@NoArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("WORKER")
@Table(name = TablesNames.WORKERS, indexes = {
        @Index(name = "idx_worker_available_position", columnList = "isAvailable, galleryPositionPriority")
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

    // by SYSTEM
    // Has the worker been active today?
    private boolean hasBeenActiveToday;
    // No more days available for the worker
    private boolean isExpired;
    // invalid status
    private boolean isInvalid;

    // by WORKERS
    // hidden by the worker himself
    private boolean isHidden;
    // Available for work by the worker himself.
    private boolean isAvailable;

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
    private String certificationStatus = "NOT_CERTIFIED"; // NOT_REQUESTED, PENDING_APPROVAL, CERTIFIED, REJECTED

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

    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id.activityDate DESC")
    private Set<WorkerDayActivity> daysHistory = new LinkedHashSet<>();

    public Worker(Email email, String password) {
        super(email, password);
    }

    public Worker(String username, Email email, String password) {
        this(email,password);
        this.username = username;
    }

    public void parseAndSetBirthdate(String birthdate) throws ParseException {
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


    public int getAge() {
        if (this.birthdate == null) return 0;
        java.time.LocalDate birthDate = (this.birthdate instanceof java.sql.Date sqlDate)
                ? sqlDate.toLocalDate()
                : this.birthdate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }


    // Option A : Si tu optes pour un système d'abonnement (Enum ou String)
    @Enumerated(EnumType.STRING)
    private SubscriptionTier subscriptionTier = SubscriptionTier.BASIC; // BASIC, STANDARD, PREMIUM

    // Option B : Si tu optes pour des ajouts de stockage à la carte (en octets)
    @Column(name = "extra_storage_purchased")
    private Long extraStoragePurchased = 0L;

    /**
     * Centralized method to calculate the maximum storage allowed.
     */
    public long getMaxStorageBytes() {
        long baseLimit = 0L;

        // Scénario 1 : Limite basée sur l'abonnement
        if (subscriptionTier != null) {
            baseLimit = switch (subscriptionTier) {
                case BASIC -> 50L * 1024 * 1024;    // 50 Mo
                case STANDARD -> 200L * 1024 * 1024;   // 200 Mo
                case PREMIUM -> 1024L * 1024 * 1024;  // 1 Go
                default -> 50L * 1024 * 1024;
            };
        } else {
            baseLimit = 50L * 1024 * 1024; // Valeur par défaut
        }

        // Scénario 2 : Ajout éventuel de stockage acheté à la volée
        long extra = (extraStoragePurchased != null) ? extraStoragePurchased : 0L;

        return baseLimit + extra;
    }

    public boolean isValid(){
        return getAge() >= 18 && !isInvalid() && !isBanned() && !isExpired() && !isHidden() && ! isLocked();
    }

    public boolean isCertified() {
        return "CERTIFIED".equals(this.certificationStatus);
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
        this.isAvailable = active;
        this.hasBeenActiveToday = active;
    }

    private enum SubscriptionTier{
        BASIC, STANDARD, PREMIUM
    }

    public UserRole getRole(){
        return UserRole.WORKER;
    }
}
