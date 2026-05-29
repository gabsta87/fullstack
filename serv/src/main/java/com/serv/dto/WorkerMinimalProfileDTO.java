package com.serv.dto;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Service;
import com.serv.database.entities.Worker;
import com.serv.service.WorkerService;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

import static com.serv.service.WorkerService.MAX_PREVIEW_THUMBS;

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
public class WorkerMinimalProfileDTO implements Comparable<WorkerMinimalProfileDTO>{

    private String id;
    private String username;
    private int    age;
    private String location;
    private String region;
    private String bodyType;
    private int    height;
    private int galleryIndex;
    private String lastRefreshed;
    private List<String> services;
    private boolean available;

    /** The 600×800 main card thumbnail — always loaded */
    private String mainThumbUrl;

    /**
     * Up to 5 preview thumbnails (400×300) for the hover carousel.
     * Loaded lazily — only requested when the user hovers the card.
     */
    private List<String> previewThumbUrls;

    // ── Constructor ───────────────────────────────────────────────────────────

    public static WorkerMinimalProfileDTO from(Worker w){
        return new WorkerMinimalProfileDTO(
            w.getId(), w.getUsername(), WorkerService.calculateAge(w.getBirthday()),
            w.getLocation(), w.getRegion(),
            w.getBodyType() != null ? w.getBodyType().toString() : null,
            w.getHeight(),
            w.getServices().stream().map(Service::getName).toList(), w.isAvailable(),
            w.getGalleryPositionIndex(),
            w.getMainPhoto() != null ? w.getMainPhoto().getMainThumbUrl() : null,
            w.getPhotos().stream().map(Photo::getPreviewThumbUrl).limit(MAX_PREVIEW_THUMBS).toList()
        );
    }

    public WorkerMinimalProfileDTO(
            UUID id, String name, int age,
            String location, String region,
            String bodyType, int height,
            List<String> services, boolean available,
            int galleryIndex,
            String mainThumbUrl, List<String> previewThumbUrls) {
        this.id               = id.toString();
        this.username = name;
        this.age              = age;
        this.location         = location;
        this.region           = region;
        this.bodyType         = bodyType;
        this.height           = height;
        this.services         = services;
        this.available        = available;
        this.galleryIndex     = galleryIndex;
        this.mainThumbUrl     = mainThumbUrl;
        this.previewThumbUrls = previewThumbUrls;
    }

    @Override
    public int compareTo(WorkerMinimalProfileDTO o) {
        // A greater index means closer to the top of the gallery.
        return o.galleryIndex - this.galleryIndex;
    }
}