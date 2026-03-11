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

    public Worker(String username, Email email, String password) {
        super(username, email, password);
    }
}
