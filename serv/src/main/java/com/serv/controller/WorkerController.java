package com.serv.controller;

import com.serv.database.entities.Service;
import com.serv.database.repositories.ServiceRepository;
import com.serv.dto.WorkerGalleryDTO;
import com.serv.dto.WorkerProfileDTO;
import com.serv.database.repositories.WorkerRepository;
import com.serv.service.WorkerGalleryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @Autowired
    private ServiceRepository serviceRepository;

    @GetMapping
    public ResponseEntity<List<WorkerGalleryDTO>> getGallery(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) List<String> bodyType,
            @RequestParam(required = false) List<String> services,
            @RequestParam(required = false) String eyeColor,
            @RequestParam(required = false) String hairColor) {

        Map<String, Object> allFilters = new HashMap<>();
        if (region != null) allFilters.put("region", region);
        if (bodyType != null) allFilters.put("bodyType", bodyType);
        if (services != null) allFilters.put("services", services);
        if (eyeColor != null) allFilters.put("eyeColor", eyeColor);
        if (hairColor != null) allFilters.put("hairColor", hairColor);

        return ResponseEntity.ok(galleryService.getGalleryPage(page, allFilters));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkerProfileDTO> getProfile(@PathVariable UUID id) {
        return workerRepository.findById(id)
                .map(w -> ResponseEntity.ok(WorkerProfileDTO.from(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/services")
    public ResponseEntity<List<String>> getWorkerServices() {
        return ResponseEntity.ok(serviceRepository.findAll().stream()
                .map(Service::getName)
                .collect(Collectors.toList()));
    }

}