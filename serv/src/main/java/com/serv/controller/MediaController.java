package com.serv.controller;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Video;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.VideoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.service.MediaStorageService;
import com.serv.service.MediaStorageService.SavedMedia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for uploading photos and videos.
 *
 * All heavy file-serving is handled by Nginx — Spring only deals with
 * saving files to disk (via MediaStorageService) and persisting the
 * resulting URLs to the database.
 *
 * Endpoints:
 *   POST /media/{workerId}/photos          — upload one or more photos
 *   POST /media/{workerId}/photos/main     — set / replace the main profile photo
 *   POST /media/{workerId}/videos          — upload one or more videos
 *   DELETE /media/{workerId}               — delete all media for a worker
 */
@RestController
@RequestMapping("/media")
public class MediaController {

    @Autowired private MediaStorageService storageService;
    @Autowired private PhotoRepository     photoRepository;
    @Autowired private VideoRepository     videoRepository;
    @Autowired private WorkerRepository    workerRepository;

    // ── Photos ────────────────────────────────────────────────────────────────

    /**
     * Upload one or more photos for a worker.
     * The first file in the list becomes the main photo if the worker
     * has no main photo yet.
     */
    @PostMapping("/{workerId}/photos")
    public ResponseEntity<List<PhotoResponse>> uploadPhotos(
            @PathVariable UUID workerId,
            @RequestParam("files") List<MultipartFile> files) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        boolean hasMain = photoRepository.existsByWorkerId(workerId);
        List<PhotoResponse> responses = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            try {
                boolean makeMain = !hasMain && i == 0;
                SavedMedia saved = storageService.savePhoto(files.get(i), workerId);

                Photo photo = new Photo();
                photo.setWorker(worker);
                photo.setOriginalUrl(saved.originalUrl());
                photo.setMainThumbUrl(saved.mainThumbUrl());
                photo.setPreviewThumbUrl(saved.previewThumbUrl());
                photoRepository.save(photo);

                if (makeMain) hasMain = true;

                responses.add(new PhotoResponse(
                        photo.getId(),
                        saved.originalUrl(),
                        saved.mainThumbUrl(),
                        saved.previewThumbUrl(),
                        makeMain
                ));
            } catch (IOException | IllegalArgumentException e) {
                responses.add(new PhotoResponse(null, null, null, null, false));
            }
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * Replace the main profile photo for a worker.
     * Demotes any existing main photo to a regular photo first.
     */
    @PostMapping("/{workerId}/photos/main")
    public ResponseEntity<PhotoResponse> setMainPhoto(
            @PathVariable UUID workerId,
            @RequestParam("file") MultipartFile file) throws IOException {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        // Demote current main
        photoRepository.findByWorkerId(workerId)
                .ifPresent(p -> photoRepository.save(p));

        SavedMedia saved = storageService.savePhoto(file, workerId);

        Photo photo = new Photo();
        photo.setWorker(worker);
        photo.setOriginalUrl(saved.originalUrl());
        photo.setMainThumbUrl(saved.mainThumbUrl());
        photo.setPreviewThumbUrl(saved.previewThumbUrl());
        photoRepository.save(photo);

        return ResponseEntity.ok(new PhotoResponse(
                photo.getId(),
                saved.originalUrl(),
                saved.mainThumbUrl(),
                saved.previewThumbUrl(),
                true
        ));
    }

    // ── Videos ────────────────────────────────────────────────────────────────

    @PostMapping("/{workerId}/videos")
    public ResponseEntity<List<VideoResponse>> uploadVideos(
            @PathVariable UUID workerId,
            @RequestParam("files") List<MultipartFile> files) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        List<VideoResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                SavedMedia saved = storageService.saveVideo(file, workerId);

                Video video = new Video();
                video.setWorker(worker);
                video.setUrl(saved.originalUrl());
                videoRepository.save(video);

                responses.add(new VideoResponse(video.getId(), saved.originalUrl()));
            } catch (IOException | IllegalArgumentException e) {
                responses.add(new VideoResponse(-1L, null));
            }
        }

        return ResponseEntity.ok(responses);
    }

    // ── Delete all media ──────────────────────────────────────────────────────

    @DeleteMapping("/{workerId}")
    public ResponseEntity<Void> deleteAll(@PathVariable UUID workerId) throws IOException {
        storageService.deleteAllForWorker(workerId);
        photoRepository.deleteByWorkerId(workerId);
        videoRepository.deleteByWorkerId(workerId);
        return ResponseEntity.noContent().build();
    }

    // ── Response DTOs ─────────────────────────────────────────────────────────

    public record PhotoResponse(
            UUID   id,
            String originalUrl,
            String mainThumbUrl,
            String previewThumbUrl,
            boolean isMain
    ) {}

    public record VideoResponse(Long id, String url) {}
}
