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
import java.util.List;
import java.util.UUID;

/**
 * Handles all media storage:
 *  - Saves original file to disk
 *  - Generates a main thumbnail  (600 × 800  — used for profile cards in the gallery)
 *  - Generates preview thumbnails (400 × 300  — used for the hover carousel on cards)
 * Directory layout on disk:
 *   ${media.upload.base}/
 *     originals/{workerId}/{uuid}.jpg
 *     thumbs/main/{workerId}/{uuid}_main.jpg
 *     thumbs/preview/{workerId}/{uuid}_prev.jpg
 * Nginx serves everything under ${media.upload.base} directly.
 * Spring never needs to stream image bytes — only metadata goes through the API.
 */
@Service
public class MediaStorageService {

    @Value("${media.upload.base}")
    private String uploadBase;

    @Value("${media.public.base-url}")
    private String publicBaseUrl;

    private static final int MAIN_THUMB_W = 600;
    private static final int MAIN_THUMB_H = 800;
    private static final int PREV_THUMB_W = 400;
    private static final int PREV_THUMB_H = 300;

    public SavedMedia savePhoto(MultipartFile file, UUID workerId) throws IOException {
        validateImage(file);

        String uuid = UUID.randomUUID().toString();
        String ext = getExtension(file.getOriginalFilename());
        String baseName = uuid + ext;

        Path originalsDir = resolveDir("originals", workerId);
        Path mainThumbDir = resolveDir("thumbs/main", workerId);
        Path prevThumbDir = resolveDir("thumbs/preview", workerId);

        Path originalPath = originalsDir.resolve(baseName).normalize();
        Path mainThumbPath = mainThumbDir.resolve(uuid + "_main" + ext).normalize();
        Path prevThumbPath = prevThumbDir.resolve(uuid + "_prev" + ext).normalize();

        // 1 — Save original
        Files.write(originalPath, file.getBytes());

        // 2 — Generate main thumbnail
        Thumbnails.of(originalPath.toFile())
                .size(MAIN_THUMB_W, MAIN_THUMB_H)
                .crop(Positions.CENTER)
                .outputQuality(0.85)
                .toFile(mainThumbPath.toFile());

        // 3 — Generate preview thumbnail
        Thumbnails.of(originalPath.toFile())
                .size(PREV_THUMB_W, PREV_THUMB_H)
                .crop(Positions.CENTER)
                .outputQuality(0.75)
                .toFile(prevThumbPath.toFile());

        return new SavedMedia(
                buildUrl("originals", workerId, baseName),
                buildUrl("thumbs/main", workerId, uuid + "_main" + ext),
                buildUrl("thumbs/preview", workerId, uuid + "_prev" + ext)
        );
    }

    public void deletePhotoFiles(UUID workerId, String originalUrl, String mainThumbUrl, String previewThumbUrl) {
        try {
            deletePhysicalFile("originals", workerId, originalUrl);
            deletePhysicalFile("thumbs/main", workerId, mainThumbUrl);
            deletePhysicalFile("thumbs/preview", workerId, previewThumbUrl);
        } catch (IOException e) {
            System.err.println("Erreur suppression physique pour le worker " + workerId + ": " + e.getMessage());
        }
    }

    private void deletePhysicalFile(String subdir, UUID workerId, String url) throws IOException {
        if (url == null || !url.contains("/")) return;

        String filename = url.substring(url.lastIndexOf('/') + 1);

        // 🛡️ SÉCURITÉ ANTI-PATH TRAVERSAL : Résolution sécurisée du chemin de base
        Path baseDir = Paths.get(uploadBase).toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(subdir).resolve(String.valueOf(workerId)).normalize();
        Path filePath = targetDir.resolve(filename).normalize();

        // Vérification absolue que le fichier cible reste bien à l'intérieur du dossier autorisé
        if (!filePath.startsWith(targetDir)) {
            throw new SecurityException("Tentative de Path Traversal détectée !");
        }

        // 1 — Supprime le fichier image s'il existe
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        // 2 — Nettoyage du dossier parent s'il est vide
        if (Files.isDirectory(targetDir)) {
            try (var entries = Files.newDirectoryStream(targetDir)) {
                if (!entries.iterator().hasNext()) {
                    Files.delete(targetDir);
                    System.out.println("Dossier vide nettoyé : " + targetDir);
                }
            }
        }
    }

    public SavedMedia saveVideo(MultipartFile file, UUID workerId) throws IOException {
        validateVideo(file);

        String uuid = UUID.randomUUID().toString();
        String ext = getExtension(file.getOriginalFilename());
        String baseName = uuid + ext;

        Path videosDir = resolveDir("videos", workerId);
        Path videoPath = videosDir.resolve(baseName).normalize();

        Files.write(videoPath, file.getBytes());

        return new SavedMedia(buildUrl("videos", workerId, baseName), null, null);
    }

    public void deleteAllForWorker(UUID workerId) throws IOException {
        Path baseDir = Paths.get(uploadBase).toAbsolutePath().normalize();

        for (String subdir : List.of("originals", "thumbs/main", "thumbs/preview", "videos")) {
            Path dir = baseDir.resolve(subdir).resolve(String.valueOf(workerId)).normalize();

            // Double vérification de sécurité
            if (dir.startsWith(baseDir) && Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Path resolveDir(String subdir, UUID workerId) throws IOException {
        Path baseDir = Paths.get(uploadBase).toAbsolutePath().normalize();
        Path dir = baseDir.resolve(subdir).resolve(String.valueOf(workerId)).normalize();

        if (!dir.startsWith(baseDir)) {
            throw new SecurityException("Chemin non autorisé.");
        }

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    private String buildUrl(String subdir, UUID workerId, String filename) {
        return publicBaseUrl + "/" + subdir + "/" + workerId + "/" + filename;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }

    private void validateImage(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are accepted.");
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("Image must be smaller than 20 MB.");
        }
    }

    private void validateVideo(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("video/")) {
            throw new IllegalArgumentException("Only video files are accepted.");
        }
        if (file.getSize() > 500L * 1024 * 1024) {
            throw new IllegalArgumentException("Video must be smaller than 500 MB.");
        }
    }

    public record SavedMedia(
            String originalUrl,
            String mainThumbUrl,
            String previewThumbUrl
    ) {}
}