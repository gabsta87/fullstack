package com.serv.database;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Data
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @Basic(optional = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Basic(optional = false)
    @Column(nullable = false)
    private String content;
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Client user;
    @Basic(optional = false)
    @Column(nullable = false)
    private Date date;

}
