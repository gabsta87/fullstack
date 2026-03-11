package com.serv.database.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "videos", indexes = {
        @Index(name = "idx_video_worker", columnList = "worker_id")
})
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private Worker worker;

    @Column(nullable = false, length = 512)
    private String url;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long    getId()               { return id; }
    public void    setId(Long id)        { this.id = id; }

    public Worker  getWorker()           { return worker; }
    public void    setWorker(Worker w)   { this.worker = w; }

    public String  getUrl()              { return url; }
    public void    setUrl(String url)    { this.url = url; }

    public Integer getSortOrder()        { return sortOrder; }
    public void    setSortOrder(Integer o) { this.sortOrder = o; }
}