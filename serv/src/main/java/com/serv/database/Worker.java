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
    @OneToMany
    Collection<Photo> photos;
    @OneToMany
    Collection<Comment> comments;
//    @ManyToMany
//    Collection<Tag> tags;

    public Worker() {}

}
