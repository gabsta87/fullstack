package com.serv.database.entities;

import com.serv.common.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;

import java.text.DateFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("WORKER")
@Table(name = TablesNames.WORKERS, indexes = {
        @Index(name = "idx_worker_available_position", columnList = "available, galleryPositionIndex")
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
    protected boolean disabled;

    private int galleryPositionIndex;

    private boolean expired;
    private boolean available;
    private boolean verified;

    // Instant stored as UTC timestamp
    @Column(name = "last_refreshed")
    private Instant lastRefreshed;

    // Date stored as DATE only (no time component)
    @Temporal(TemporalType.DATE)
    @Column(name = "birthdate")
    private Date birthdate;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Subscription expiry date — null means never subscribed.
     * Set this when a payment is confirmed.
     */
    @Column(name = "subscription_expires_at")
    private Instant subscriptionExpiresAt;

    public Worker(String username, Email email, String password) {
        super(username, email, password);
        this.role = UserRole.WORKER;
    }

    public Worker() {
        this.role = UserRole.WORKER;
    }

    /**
     * Returns remaining subscription days (0 if expired or never subscribed).
     * Used by the profile management page to show the subscription badge.
     */
    public long getSubscriptionDaysLeft() {
        if (subscriptionExpiresAt == null) return 0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(Instant.now(), subscriptionExpiresAt);
        return Math.max(0, days);
    }

    /**
     * Called when a payment is confirmed — adds `days` to the current expiry
     * (or from now if already expired).
     */
    public void extendSubscription(long days) {
        Instant base = (subscriptionExpiresAt != null && subscriptionExpiresAt.isAfter(Instant.now()))
                ? subscriptionExpiresAt
                : Instant.now();
        this.subscriptionExpiresAt = base.plus(days, java.time.temporal.ChronoUnit.DAYS);
        this.expired = getSubscriptionDaysLeft() <= 0;
    }

    public void parseBirthdate(String birthdate) throws ParseException {
        this.birthdate = DateFormat.getDateInstance().parse(birthdate);
    }

    public void addPhoto(Photo photo) {
        this.photos.add(photo);
    }

    public void removePhoto(Photo photo) {
        this.photos.remove(photo);
    }

}
