package com.serv.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
public class Comment {
    @Id
    private long id;
    private String content;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private Client user;
    private Date date;

}
