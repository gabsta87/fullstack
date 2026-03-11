package com.serv.controller;

import com.serv.database.repositories.PhotoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Adds the GET /workers/{id}/previews endpoint that the Angular
 * WorkerService.getPreviewThumbs() calls lazily on card hover.
 *
 * Returns a plain list of preview thumbnail URLs (strings).
 * Spring serialises this as a JSON array: ["url1","url2",...]
 *
 * Add this method to your existing WorkerController,
 * or keep it here as a separate controller — either works.
 */
@RestController
@RequestMapping("/workers")
public class WorkerPreviewController {

    @Autowired
    private PhotoRepository photoRepository;

    /**
     * Returns up to 5 preview thumbnail URLs for the hover carousel.
     * Only previewThumbUrl strings — no other data needed by the frontend.
     */
    @GetMapping("/{id}/previews")
    public ResponseEntity<List<String>> getPreviews(@PathVariable UUID id) {
        List<String> urls = photoRepository
                .findByWorkerIdOrderBySortOrderAscIdAsc(id)
                .stream()
                .map(p -> p.getPreviewThumbUrl())
                .filter(url -> url != null && !url.isBlank())
                .limit(5)
                .toList();

        return ResponseEntity.ok(urls);
    }
}