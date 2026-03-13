package com.serv.controller;

import com.serv.dto.WorkerGalleryDTO;
import com.serv.dto.WorkerProfileDTO;
import com.serv.database.repositories.WorkerRepository;
import com.serv.service.WorkerGalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Worker-facing read endpoints.
 *
 * GET /workers          — paginated gallery (lightweight DTOs, main thumb only)
 * GET /workers/{id}     — full profile (all photos, videos, reviews…)
 */
@RestController
@RequestMapping("/workers")
@Transactional(readOnly = true)   // ← add this
public class WorkerController {
    @Autowired private WorkerRepository    workerRepository;
    @Autowired private WorkerGalleryService galleryService;

    @GetMapping
    public ResponseEntity<List<WorkerGalleryDTO>> getGallery(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam Map<String, String> filters) {
        filters.remove("page");
        return ResponseEntity.ok(galleryService.getGalleryPage(page, filters));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkerProfileDTO> getProfile(@PathVariable UUID id) {
        return workerRepository.findById(id)
                .map(w -> ResponseEntity.ok(WorkerProfileDTO.from(w)))
                .orElse(ResponseEntity.notFound().build());
    }
}