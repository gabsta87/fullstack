package com.serv.controller;

import com.serv.database.entities.Media;
import com.serv.database.entities.Photo;
import com.serv.database.entities.Video;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.PhotoRepository;
import com.serv.database.repositories.VideoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.service.MediaStorageService;
import com.serv.service.MediaStorageService.SavedMedia;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for uploading photos and videos.
 * All heavy file-serving is handled by Nginx — Spring only deals with
 * saving files to disk (via MediaStorageService) and persisting the
 * resulting URLs to the database.
 * Endpoints:
 *   POST /media/{workerId}/photos          — upload one or more photos
 *   POST /media/{workerId}/photos/main     — set / replace the main profile photo
 *   POST /media/{workerId}/videos          — upload one or more videos
 *   DELETE /media/{workerId}               — delete all media for a worker
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
public class MediaController {

    private final MediaStorageService storageService;
    private final PhotoRepository     photoRepository;
    private final VideoRepository     videoRepository;
    private final WorkerRepository    workerRepository;

    // ── Medias ────────────────────────────────────────────────────────────────
    @PostMapping("/{workerId}/media")
    public ResponseEntity<?> uploadMedia(
            @PathVariable UUID workerId,
            @RequestParam("files") List<MultipartFile> files) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));

        // Calcul du stockage actuel (Vidéos + Photos)
        long currentUsedStorage = videoRepository.findByWorkerId(workerId).stream()
                .mapToLong(Media::getFileSize)
                .sum()
                +
                photoRepository.findByWorkerId(workerId).stream()
                        .mapToLong(Media::getFileSize)
                        .sum();

        List<Object> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            long fileSize = file.getSize();
            String contentType = file.getContentType();

            // Vérification globale du quota de stockage
            if (currentUsedStorage + fileSize > worker.getMaxStorageBytes()) {
                return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE)
                        .body("Storage limit reached : " + (worker.getMaxStorageBytes() / (1024 * 1024)) + " Mo.");
            }

            try {
                if (contentType != null && contentType.startsWith("image")) {
                    // --- Traitement PHOTO ---
                    SavedMedia saved = storageService.savePhoto(file, workerId);

                    Photo photo = new Photo();
                    photo.setWorker(worker);
                    photo.setUrl(saved.originalUrl());
                    photo.setMainThumbUrl(saved.mainThumbUrl());
                    photo.setPreviewThumbUrl(saved.previewThumbUrl());
                    photo.setFileSize(fileSize);
                    photoRepository.save(photo);

                    currentUsedStorage += fileSize;
                    responses.add(new PhotoResponse(photo.getId(), saved.originalUrl(), saved.mainThumbUrl(), saved.previewThumbUrl(), false));

                } else if (contentType != null && contentType.startsWith("video")) {
                    // --- Traitement VIDÉO ---
                    SavedMedia saved = storageService.saveVideo(file, workerId);

                    Video video = new Video();
                    video.setWorker(worker);
                    video.setUrl(saved.originalUrl());
                    video.setFileSize(fileSize);
                    videoRepository.save(video);

                    currentUsedStorage += fileSize;
                    responses.add(new VideoResponse(video.getId(), saved.originalUrl()));
                }
                // other formats not supported yet

            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
                // Gestion d'erreur unitaire si besoin
            }
        }

        return ResponseEntity.ok(responses);
    }

    // ── Delete all media ──────────────────────────────────────────────────────

    @DeleteMapping("/{workerId}")
    public ResponseEntity<Void> deleteAll(@PathVariable UUID workerId) throws IOException {
        if( ! workerRepository.existsById(workerId)){
            storageService.deleteAllForWorker(workerId);
            photoRepository.deleteByWorkerId(workerId);
            videoRepository.deleteByWorkerId(workerId);
        }
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

    public record VideoResponse(UUID id, String url) {}
}
