package com.serv.dto;

import com.serv.common.BodyType;
import com.serv.common.Service;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight DTO returned by GET /workers (the main gallery endpoint).
 *
 * Only contains:
 *  - Core profile fields needed to render a gallery card
 *  - The main thumbnail URL (served by Nginx, not Spring)
 *  - A list of preview thumbnail URLs for the hover carousel
 *
 * Full profile data (all photos, videos, description, reviews…)
 * is fetched separately via GET /workers/{id} only when the user
 * clicks through to the profile page.
 */
@Getter
@Setter
public class WorkerGalleryDTO {

    private UUID id;
    private String name;
    private Date   birthday;
    private String location;
    private String region;
    private BodyType bodyType;
    private int    height;
    private List<Service> services;
    private boolean available;
    private Instant lastRefreshed;

    /** The 600×800 main card thumbnail — always loaded */
    private String mainThumbUrl;

    /**
     * Up to 5 preview thumbnails (400×300) for the hover carousel.
     * Loaded lazily — only requested when the user hovers the card.
     */
    private List<String> previewThumbUrls;

    // ── Constructor ───────────────────────────────────────────────────────────

    public WorkerGalleryDTO() {}

    public WorkerGalleryDTO(
            UUID id, String name, Date birthday,
            String location, String region,
            BodyType bodyType, int height,
            List<Service> services, boolean available,
            Instant lastRefreshed,
            String mainThumbUrl, List<String> previewThumbUrls) {
        this.id               = id;
        this.name             = name;
        this.birthday         = birthday;
        this.location         = location;
        this.region           = region;
        this.bodyType         = bodyType;
        this.height           = height;
        this.services         = services;
        this.available        = available;
        this.lastRefreshed    = lastRefreshed;
        this.mainThumbUrl     = mainThumbUrl;
        this.previewThumbUrls = previewThumbUrls;
    }
}