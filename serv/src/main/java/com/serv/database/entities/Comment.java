package com.serv.database.entities;

import com.serv.common.TablesNames;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = TablesNames.COMMENTS)
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
    @ManyToOne
    @JoinColumn(name = "worker_id")
    private Worker worker;

}
