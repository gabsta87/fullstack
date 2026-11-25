package com.serv.database.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.serv.common.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;

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
    @SQLRestriction("photo_type = 'GALLERY'")
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
    private boolean isExpired = false;
    // invalid status
    private boolean isInvalid;

    // by WORKERS
    // hidden by the worker himself. The profile won't appear in the gallery
    private boolean isHidden = false;
    // Available for work by the worker himself. If not available, the profile will still appear, but as unavailable
    private boolean isAvailable = true;

    // Instant stored as UTC timestamp
    @Column(name = "last_refreshed")
    private Instant lastRefreshed;

    // SQL DATE (no time component, no time zone)
    @Column(name = "birthdate")
    private java.time.LocalDate birthdate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 50)
    private String shortDescription;

    @Column(name = "remaining_days_credit")
    private Integer remainingDaysCredit;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "certified_at")
    private LocalDateTime certifiedAt;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "last_certification_request_id")
    private CertificationRequest lastCertificationRequest;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "workers_languages",
            joinColumns = @JoinColumn(name = "worker_id")
    )
    private Set<WorkerLanguage> spokenLanguages = new HashSet<>();

    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Report> reports = new ArrayList<>();

    public Worker(Email email, String password) {
        super(email, password, null);
    }

    public Worker(String username, Email email, String password) {
        this(email,password);
        this.username = username;
    }

    public static final int MAX_AGE = 120;

    private static final java.time.format.DateTimeFormatter ISO_DATE =
            java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private static final java.time.format.DateTimeFormatter FR_DATE =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT);

    /**
     * Parses a birthdate in {@code yyyy-MM-dd} or {@code dd/MM/yyyy} format. Blank clears the birthdate.
     *
     * @throws IllegalArgumentException for an unparsable or impossible date (e.g. 2000-02-30),
     *                                  a date in the future, or an age above {@value #MAX_AGE}
     */
    public void parseAndSetBirthdate(String birthdate) {
        if (birthdate == null || birthdate.isBlank()) {
            this.birthdate = null;
            return;
        }
        String value = birthdate.trim();
        java.time.LocalDate date;
        try {
            date = value.matches("\\d{4}-\\d{2}-\\d{2}")
                    ? java.time.LocalDate.parse(value, ISO_DATE)
                    : java.time.LocalDate.parse(value, FR_DATE);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid birthdate format.");
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        if (date.isAfter(today) || date.isBefore(today.minusYears(MAX_AGE))) {
            throw new IllegalArgumentException("Invalid birthdate.");
        }
        this.birthdate = date;
    }


    public int getAge() {
        if (this.birthdate == null) return 0;
        return java.time.Period.between(this.birthdate, java.time.LocalDate.now()).getYears();
    }


    // Option A: subscription tier
    @Enumerated(EnumType.STRING)
    private SubscriptionTier subscriptionTier = SubscriptionTier.BASIC; // BASIC, STANDARD, PREMIUM

    // Option B: extra storage bought separately (in bytes)
    @Column(name = "extra_storage_purchased")
    private Long extraStoragePurchased = 0L;

    /**
     * Centralized method to calculate the maximum storage allowed.
     */
    public long getMaxStorageBytes() {
        long baseLimit = 0L;

        // Case 1: limit based on the subscription tier
        if (subscriptionTier != null) {
            baseLimit = switch (subscriptionTier) {
                case BASIC -> 50L * 1024 * 1024;    // 50 Mo
                case STANDARD -> 200L * 1024 * 1024;   // 200 Mo
                case PREMIUM -> 1024L * 1024 * 1024;  // 1 Go
                default -> 50L * 1024 * 1024;
            };
        } else {
            baseLimit = 50L * 1024 * 1024; // Default value
        }

        // Case 2: extra storage bought separately
        long extra = (extraStoragePurchased != null) ? extraStoragePurchased : 0L;

        return baseLimit + extra;
    }

    public boolean isValid(){
        return getAge() >= 18 && !isInvalid() && !isBanned() && !isExpired() && !isHidden() && ! isLocked();
    }

    public boolean isCertified() {
        if (this.certifiedAt == null) {
            return false;
        }
        return this.certifiedAt.plusMonths(8).isAfter(LocalDateTime.now());
    }

    public void addPhoto(Photo photo) {
        this.photos.add(photo);
    }

    public void removePhoto(Photo photo) {
        this.photos.remove(photo);
    }

    public void addVideo(Video video) {this.videos.add(video);}

    public void removeVideo(Video video) {this.videos.remove(video);}

    public void addSpokenLanguage(WorkerLanguage language) {this.spokenLanguages.add(language);}

    public void setActive(boolean active) {
        this.isAvailable = active;
        this.hasBeenActiveToday = active;
    }

    public void clearCertification() {
        CertificationRequest request = this.getLastCertificationRequest();
        if (request != null) {
            request.setStatus(CertificationStatus.NOT_CERTIFIED);
        }
        this.setCertifiedAt(null);
        this.setVerificationCode(null);
    }

    public void removeComment(Comment comment) {
        this.comments.remove(comment);
        comment.setWorker(null);
    }

    public void setShortDescription(String shortDescription) {
        if (shortDescription != null && shortDescription.length() > 45) {
            // Truncates with an ellipsis when the text is too long
            this.shortDescription = shortDescription.substring(0, 42) + "...";
        } else {
            this.shortDescription = shortDescription;
        }
    }

    private enum SubscriptionTier{
        BASIC, STANDARD, PREMIUM
    }

    public UserRole getRole(){
        return UserRole.WORKER;
    }
}
