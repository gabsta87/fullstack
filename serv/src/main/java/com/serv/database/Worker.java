package com.serv.database;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@Entity
public class Worker extends User{

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
//    @ManyToMany
//    Collection<Tag> tags;
//    @OneToMany
//    Collection<Video> videos;

    public Worker() {}

}
