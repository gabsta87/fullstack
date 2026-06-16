package com.serv.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;

@Data
@Entity
@Table(name = "geographic_zones")
public class GeographicZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    // La zone parente (ex: Le Canton pour une Commune, ou la Région pour un Département)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @JsonIgnore
    private GeographicZone parent;

    // Les sous-zones rattachées
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Collection<GeographicZone> subZones = new ArrayList<>();

    public GeographicZone(String name, GeographicZone parent) {
        this.name = name;
        this.parent = parent;
    }

    public GeographicZone() {}
}