package com.serv.database.entities;

import com.serv.common.BodyType;
import com.serv.common.Service;
import com.serv.common.ServiceListConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("SELLER")
@Table(name = "sellers")
public class Worker extends VenusUser {

    @Embedded
    private PhysicalAddress address;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_photo_id")
    private Photo mainPhoto;

    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Photo> photos = new ArrayList<>();

    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<Comment> comments = new ArrayList<>();

    private int priority;

    @Convert(converter = ServiceListConverter.class)
    @Column(name = "services", length = 64)
    private List<Service> services = new ArrayList<>();

    private boolean expired;
    private boolean available;

    // Instant stored as UTC timestamp
    @Column(name = "last_refreshed")
    private Instant lastRefreshed;

    private String region;
    private String location;

    // Enum stored as String
    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", length = 16)
    private BodyType bodyType;

    private int height;
    private int weight;

    // Date stored as DATE only (no time component)
    @Temporal(TemporalType.DATE)
    @Column(name = "birthday")
    private Date birthday;

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
}
