package com.serv.service;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles all media storage:
 *  - Saves original file to disk
 *  - Generates a main thumbnail  (600 × 800  — used for profile cards in the gallery)
 *  - Generates preview thumbnails (400 × 300  — used for the hover carousel on cards)
 *
 * Directory layout on disk:
 *   ${media.upload.base}/
 *     originals/{workerId}/{uuid}.jpg
 *     thumbs/main/{workerId}/{uuid}_main.jpg
 *     thumbs/preview/{workerId}/{uuid}_prev.jpg
 *
 * Nginx serves everything under ${media.upload.base} directly.
 * Spring never needs to stream image bytes — only metadata goes through the API.
 */
@Service
public class MediaStorageService {

    // Injected from application.properties
    @Value("${media.upload.base}")
    private String uploadBase;

    // Public base URL that Nginx exposes for this directory
    // e.g. https://yourdomain.com/media  or  http://localhost:8080/media (dev)
    @Value("${media.public.base-url}")
    private String publicBaseUrl;

    // ── Thumbnail dimensions ──────────────────────────────────────────────────

    /** Card thumbnail — portrait, shown in the main gallery grid */
    private static final int MAIN_THUMB_W = 600;
    private static final int MAIN_THUMB_H = 800;

    /** Preview thumbnail — shown in the hover carousel on gallery cards */
    private static final int PREV_THUMB_W = 400;
    private static final int PREV_THUMB_H = 300;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves one photo and returns a {@link SavedMedia} record containing
     * the public URLs for the original, main thumb and preview thumb.
     *
     * @param file      the uploaded file
     * @param workerId  ID of the worker this photo belongs to
     */
    public SavedMedia savePhoto(MultipartFile file, UUID workerId) throws IOException {

        validateImage(file);

        String uuid     = UUID.randomUUID().toString();
        String ext      = getExtension(file.getOriginalFilename());
        String baseName = uuid + ext;

        // Resolve disk paths
        Path originalsDir  = resolveDir("originals",     workerId);
        Path mainThumbDir  = resolveDir("thumbs/main",   workerId);
        Path prevThumbDir  = resolveDir("thumbs/preview",workerId);

        Path originalPath  = originalsDir .resolve(baseName);
        Path mainThumbPath = mainThumbDir .resolve(uuid + "_main" + ext);
        Path prevThumbPath = prevThumbDir .resolve(uuid + "_prev" + ext);

        // 1 — Save original
        Files.write(originalPath, file.getBytes());

        // 2 — Generate main card thumbnail (cropped to portrait ratio)
        Thumbnails.of(originalPath.toFile())
                .size(MAIN_THUMB_W, MAIN_THUMB_H)
                .crop(Positions.CENTER)
                .outputQuality(0.85)
                .toFile(mainThumbPath.toFile());

        // 3 — Generate small preview thumbnail (landscape, for hover carousel)
        Thumbnails.of(originalPath.toFile())
                .size(PREV_THUMB_W, PREV_THUMB_H)
                .crop(Positions.CENTER)
                .outputQuality(0.75)
                .toFile(prevThumbPath.toFile());

        return new SavedMedia(
                buildUrl("originals",      workerId, baseName),
                buildUrl("thumbs/main",    workerId, uuid + "_main" + ext),
                buildUrl("thumbs/preview", workerId, uuid + "_prev" + ext)
        );
    }

    /**
     * Saves a video file (no thumbnail generation — handled client-side or separately).
     */
    public SavedMedia saveVideo(MultipartFile file, UUID workerId) throws IOException {

        validateVideo(file);

        String uuid     = UUID.randomUUID().toString();
        String ext      = getExtension(file.getOriginalFilename());
        String baseName = uuid + ext;

        Path videosDir = resolveDir("videos", workerId);
        Files.write(videosDir.resolve(baseName), file.getBytes());

        String videoUrl = buildUrl("videos", workerId, baseName);
        return new SavedMedia(videoUrl, null, null);
    }

    /**
     * Deletes all files associated with a worker (originals + all thumbnails).
     * Call this when a worker account is deleted.
     */
    public void deleteAllForWorker(UUID workerId) throws IOException {
        for (String subdir : List.of("originals", "thumbs/main", "thumbs/preview", "videos")) {
            Path dir = Paths.get(uploadBase, subdir, String.valueOf(workerId));
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Resolves a subdirectory path and creates it if missing. */
    private Path resolveDir(String subdir, UUID workerId) throws IOException {
        Path dir = Paths.get(uploadBase, subdir, String.valueOf(workerId));
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    /** Builds the public Nginx URL for a given file. */
    private String buildUrl(String subdir, UUID workerId, String filename) {
        return publicBaseUrl + "/" + subdir + "/" + workerId + "/" + filename;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private void validateImage(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || (!ct.startsWith("image/"))) {
            throw new IllegalArgumentException("Only image files are accepted.");
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("Image must be smaller than 20 MB.");
        }
    }

    private void validateVideo(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || (!ct.startsWith("video/"))) {
            throw new IllegalArgumentException("Only video files are accepted.");
        }
        if (file.getSize() > 500L * 1024 * 1024) {
            throw new IllegalArgumentException("Video must be smaller than 500 MB.");
        }
    }

    // ── Inner record ──────────────────────────────────────────────────────────

    /**
     * Immutable result of a save operation.
     * Store these URLs in your Photo / Video JPA entity.
     */
    public record SavedMedia(
            String originalUrl,
            String mainThumbUrl,   // null for videos
            String previewThumbUrl // null for videos
    ) {}
}