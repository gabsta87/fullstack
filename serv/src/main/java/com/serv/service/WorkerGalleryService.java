package com.serv.service;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.dto.WorkerGalleryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkerGalleryService {

    @Autowired private WorkerRepository workerRepository;
    @Autowired private PhotoRepository  photoRepository;

    private static final int MAX_PREVIEW_THUMBS = 5;
    private static final int PAGE_SIZE          = 24;

    /**
     * Returns a page of WorkerGalleryDTOs.
     *
     * Ordering rule:
     *   1. Available workers first, ordered by lastRefreshed DESC
     *   2. Unavailable workers after, same ordering
     *
     * For each worker we attach:
     *   - mainThumbUrl     : the main photo's mainThumbUrl
     *   - previewThumbUrls : up to MAX_PREVIEW_THUMBS previewThumbUrls
     *                        (all photos, not just main)
     *
     * This is done in two queries (workers + batch photo fetch) to avoid N+1.
     */
    public List<WorkerGalleryDTO> getGalleryPage(int page, Map<String, Object> filters) {

        // Pour l'instant, ton code ignore les filtres.
        // Si tu veux voir tes profils, il faut que l'appel SQL soit filtré.
        // Voici une version simplifiée pour tester si les profils reviennent :

        // 1 — Fetch workers (available first, then by lastRefreshed)
        List<Worker> available = workerRepository.findByAvailableTrue(
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "lastRefreshed")));

        List<Worker> unavailable = workerRepository
                .findByAvailableFalse(
                        PageRequest.of(page, PAGE_SIZE,
                                Sort.by(Sort.Direction.DESC, "lastRefreshed")));

        List<Worker> workers = new ArrayList<>();
        workers.addAll(available);
        workers.addAll(unavailable);
        workers = workers.subList(0, Math.min(PAGE_SIZE, workers.size()));

        if (workers.isEmpty()) return List.of();

        // 2 — Main thumbs come directly from the worker (no extra query)
        Map<UUID, String> mainThumbs = workers.stream()
                .filter(w -> w.getMainPhoto() != null)
                .collect(Collectors.toMap(
                        Worker::getId,
                        w -> w.getMainPhoto().getMainThumbUrl()
                ));

        // Preview thumbs — batch fetch, grouped by workerId
        List<UUID> workerIds = workers.stream().map(Worker::getId).toList();
        Map<UUID, List<String>> previewThumbs = new HashMap<>();
        photoRepository
                .findByWorkerIdInOrderBySortOrderAsc(workerIds)
                .forEach(p -> previewThumbs
                        .computeIfAbsent(p.getWorker().getId(), k -> new ArrayList<>())
                        .add(p.getPreviewThumbUrl()));

        // 3 — Assemble DTOs
        return workers.stream().map(w -> new WorkerGalleryDTO(
                w.getId(),         // UUID → String for JSON
                w.getUsername(),
                w.getBirthday(),// Date → int age
                w.getLocation(),
                w.getRegion(),
                w.getBodyType(),
                w.getHeight(),
                w.getServices().stream().map(s -> s.getName()).toList(),
                w.isAvailable(),
                w.getLastRefreshed(),
                mainThumbs.getOrDefault(w.getId(), null),
                previewThumbs.getOrDefault(w.getId(), List.of())
                        .stream().limit(MAX_PREVIEW_THUMBS).toList()
        )).toList();
    }

    private int calculateAge(java.util.Date birthday) {
        if (birthday == null) return 0;
        java.time.LocalDate birthDate = (birthday instanceof java.sql.Date sqlDate)
                ? sqlDate.toLocalDate()
                : birthday.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }

    /**
     * Returns gallery DTOs for a specific list of worker IDs.
     * Used by GET /account/favorites to return a client's saved workers.
     */
    @Transactional(readOnly = true)
    public List<WorkerGalleryDTO> getGalleryByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return workerRepository.findAllById(ids).stream()
                .map(this::toDTO)
                .toList();
    }

    private WorkerGalleryDTO toDTO(Worker w) {
        String mainThumbUrl = w.getMainPhoto() != null
                ? w.getMainPhoto().getMainThumbUrl()
                : null;

        List<String> previewThumbs = photoRepository
                .findByWorkerIdInOrderBySortOrderAsc(List.of(w.getId()))
                .stream()
                .map(Photo::getPreviewThumbUrl)
                .limit(MAX_PREVIEW_THUMBS)
                .toList();

        List<String> serviceNames = w.getServices().stream()
                .map(s -> s.getName()) // Remplacez par le nom de l'attribut dans Service.java
                .toList();

        return new WorkerGalleryDTO(
                w.getId(),
                w.getUsername(),
                w.getBirthday(),
                w.getLocation(),
                w.getRegion(),
                w.getBodyType(),
                w.getHeight(),
                serviceNames,
                w.isAvailable(),
                w.getLastRefreshed(),
                mainThumbUrl,
                previewThumbs
        );
    }

}