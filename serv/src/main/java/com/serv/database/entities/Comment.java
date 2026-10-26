package com.serv.database.entities;

import com.serv.common.TablesNames;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Data
@Entity
@NoArgsConstructor
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
    private Client author;
    @Basic(optional = false)
    @Column(nullable = false)
    private Instant date;
    @ManyToOne
    @JoinColumn(name = "worker_id")
    private Worker worker;

    public Comment(Client author, Worker target, String content) {
        this.author = author;
        this.worker = target;
        this.content = content;
        this.date = Instant.now();
    }

    public void refreshDate() {
        this.date = Instant.now();
    }
}
