package com.serv.service;

import com.serv.database.entities.Worker;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.database.repositories.filters.WorkerSpecifications;
import com.serv.dto.WorkerMinimalProfileDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkerService {

    @Autowired private WorkerRepository workerRepository;
    @Autowired private PhotoRepository  photoRepository;

    public static final int MAX_PREVIEW_THUMBS = 5;
    public static final int PAGE_SIZE          = 24;

    /**
     * Returns a page of WorkerMinimalProfileDTOs
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
    public List<WorkerMinimalProfileDTO> getGalleryPage(int page, Map<String, Object> filters) {

        Specification<Worker> spec = Specification
                .where(WorkerSpecifications.isAvailable())
                .and(WorkerSpecifications.isNotDisabled());

        // TODO apply filters to the query
        // TODO sorting by galleryIndex desc

        // 1 — Fetch workers (available first, then by lastRefreshed)
        List<Worker> available = workerRepository.findAll(
                spec,
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "galleryPositionIndex"))
        ).getContent();

        System.out.println("GetGalleryPage : " + Arrays.toString(available.stream().map(Worker::getUsername).toArray()));
        spec = Specification
                .where(WorkerSpecifications.isNotAvailable())
                .and(WorkerSpecifications.isNotDisabled());

        List<Worker> unavailable = workerRepository.findAll(
                spec,
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "galleryPositionIndex"))
        ).getContent();

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
        return workers.stream()
                .map(WorkerMinimalProfileDTO::from)
                .sorted(WorkerMinimalProfileDTO::compareTo)
                .toList();
    }

    public static int calculateAge(java.util.Date birthday) {
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
    public List<WorkerMinimalProfileDTO> getGallery(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        List<Worker> all = workerRepository.findAllById(ids).stream()
                .toList();

        System.out.println("GetGallery all : "+Arrays.toString(all.stream().map(Worker::getUsername).toArray()));

        List<Worker> available = workerRepository.findAllById(ids).stream()
                .filter(worker -> !worker.isDisabled())
                .toList();

        System.out.println("GetGallery filtered : "+Arrays.toString(available.stream().map(Worker::getUsername).toArray()));

        return workerRepository.findAllById(ids).stream().filter(worker -> !worker.isDisabled())
                .map(WorkerMinimalProfileDTO::from)
                .sorted(WorkerMinimalProfileDTO::compareTo)
                .toList();
    }

}