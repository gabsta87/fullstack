package com.serv.database;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("SELLER")
@Table(name = "sellers")
public class Seller extends VenusUser {

    @Embedded
    private PhysicalAddress address;
    @ManyToOne
    @JoinColumn(name = "main_photo_id")
    private Photo mainPhoto;
    @OneToMany
    Collection<Photo> photos;
    @OneToMany
    Collection<Comment> comments;
    private int priority;
    private boolean expired;

    public Seller(String username, Email email, String password) {
        super(username, email, password);
    }

//    @ManyToMany
//    Collection<Tag> tags;
//    @OneToMany
//    Collection<Video> videos;

}
