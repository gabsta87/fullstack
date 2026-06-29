package com.serv.controller;

import com.serv.common.Requests;
import com.serv.database.entities.Service;
import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.ServiceRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.dto.GeographicZoneDTO;
import com.serv.dto.WorkerMinimalProfileDTO;
import com.serv.dto.WorkerPublicFullProfileDTO;
import com.serv.service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Autowired private WorkerRepository workerRepository;
    @Autowired private WorkerService    galleryService;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private GeographicZoneRepository zoneRepository;

    @GetMapping
    public ResponseEntity<List<WorkerMinimalProfileDTO>> getGallery(Requests.WorkerSearchRequest req) {
        Map<String, Object> allFilters = new HashMap<>();

        if (req.zoneId() != null && !req.zoneId().isBlank() && !req.zoneId().equalsIgnoreCase("undefined")) {
            try {
                Integer parsedZoneId = Integer.parseInt(req.zoneId());
                allFilters.put("zoneId", parsedZoneId);
            } catch (NumberFormatException ignored) {}
        }

        // Liaison des autres filtres s'ils sont fournis
        if (req.username() != null && !req.username().isBlank()) allFilters.put("username", req.username());
        if (req.bodyType() != null && !req.bodyType().isBlank()) allFilters.put("bodyType", req.bodyType());
        if (req.services() != null && !req.services().isEmpty()) allFilters.put("services", req.services());
        if (req.eyeColor() != null && !req.eyeColor().isBlank()) allFilters.put("eyeColor", req.eyeColor());
        if (req.hairColor() != null && !req.hairColor().isBlank()) allFilters.put("hairColor", req.hairColor());

        return ResponseEntity.ok(galleryService.getGalleryPage(req.getPageOrZero(), allFilters));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkerPublicFullProfileDTO> getProfile(@PathVariable UUID id) {
        return workerRepository.findByIdWithPhotos(id)
                .map(w -> ResponseEntity.ok(WorkerPublicFullProfileDTO.from(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/services")
    public ResponseEntity<List<String>> getWorkerServices() {
        return ResponseEntity.ok(serviceRepository.findAll().stream()
                .map(Service::getName)
                .collect(Collectors.toList()));
    }

    @GetMapping("/locations")
    public ResponseEntity<List<GeographicZoneDTO>> getLocationsTree() {
        List<GeographicZoneDTO> roots = zoneRepository.findAll().stream()
                .filter(z -> z.getParent() == null)
                .map(GeographicZoneDTO::from)
                .toList();
        return ResponseEntity.ok(roots);
    }
}