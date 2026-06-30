package com.serv.controller;

import com.serv.common.Requests;
import com.serv.database.entities.Photo;
import com.serv.database.entities.Service;
import com.serv.database.repositories.GeographicZoneRepository;
import com.serv.database.repositories.PhotoRepository;
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
    @Autowired private PhotoRepository photoRepository;

    @GetMapping
    public ResponseEntity<List<WorkerMinimalProfileDTO>> getGallery(Requests.WorkerSearchRequest req) {
        Map<String, Object> allFilters = new HashMap<>();

        System.out.println("Request received with filters : " + req);

        // 1. Cas particulier de la ZoneId (qui nécessite ton parsing String -> Integer de sécurité)
        if (req.zoneId() != null && !req.zoneId().isBlank() && !req.zoneId().equalsIgnoreCase("undefined")) {
            try {
                allFilters.put("zoneId", Integer.parseInt(req.zoneId()));
            } catch (NumberFormatException ignored) {}
        }

        // 2. 🎯 AUTOMATISATION DU RESTE DES FILTRES PAR RÉFLEXION
        // Parcourt toutes les méthodes/champs du record "WorkerSearchRequest"
        for (java.lang.reflect.Method method : req.getClass().getDeclaredMethods()) {
            try {
                String name = method.getName();

                // On ignore les méthodes techniques et la zoneId déjà gérée
                if (name.equals("page") || name.equals("getPageOrZero") || name.equals("zoneId") || name.equals("equals") || name.equals("hashCode") || name.equals("toString")) {
                    continue;
                }

                Object value = method.invoke(req);

                if (value != null) {
                    if (value instanceof String str && !str.isBlank()) {
                        allFilters.put(name, str.trim());
                    } else if (value instanceof List<?> list && !list.isEmpty()) {
                        allFilters.put(name, list);
                    }
                }
            } catch (Exception ignored) {
                // Sécurité en cas de problème d'accès aux méthodes du record
            }
        }

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

    /**
     * Returns up to 5 preview thumbnail URLs for the hover carousel.
     * Only previewThumbUrl strings — no other data needed by the frontend.
     */
    @GetMapping("/{id}/previews")
    public ResponseEntity<List<String>> getPreviews(@PathVariable UUID id) {
        List<String> urls = photoRepository
                .findByWorkerIdOrderBySortOrderAscIdAsc(id)
                .stream()
                .map(Photo::getPreviewThumbUrl)
                .filter(url -> url != null && !url.isBlank())
                .limit(5)
                .toList();

        return ResponseEntity.ok(urls);
    }
}