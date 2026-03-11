package com.serv.controller;

import com.serv.dto.WorkerGalleryDTO;
import com.serv.service.WorkerGalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Worker-facing read endpoints.
 *
 * GET /workers          — paginated gallery (lightweight DTOs, main thumb only)
 * GET /workers/{id}     — full profile (all photos, videos, reviews…)
 */
@RestController
@RequestMapping("/workers")
public class WorkerController {

    @Autowired private WorkerGalleryService galleryService;
    // @Autowired private WorkerProfileService profileService; // TODO

    /**
     * Returns a page of gallery cards.
     *
     * Query params:
     *   page       (default 0)
     *   region     filter by region name
     *   bodyType   filter
     *   heightMin, heightMax
     *   weightMin, weightMax
     *   eyeColor, hairColor
     *   services   comma-separated list
     */
    @GetMapping
    public ResponseEntity<List<WorkerGalleryDTO>> getGallery(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam Map<String, String> filters) {

        filters.remove("page"); // already extracted above
        return ResponseEntity.ok(galleryService.getGalleryPage(page, filters));
    }

    /**
     * Full profile — called when the user clicks a card.
     * Returns all photos, videos, description, reviews, etc.
     * TODO: implement WorkerProfileService and WorkerProfileDTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        // return ResponseEntity.ok(profileService.getById(id));
        return ResponseEntity.ok().build(); // placeholder
    }
}