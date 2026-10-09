package com.serv.service;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Video;
import com.serv.database.entities.Worker;
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

    // Nouvelle méthode pour cibler directement /{uploadBase}/{userId}/{subdir}
    private Path resolveDir(UUID userId, String subdir) throws IOException {
        Path baseDir = Paths.get(uploadBase).toAbsolutePath().normalize();
        Path dir = baseDir.resolve(String.valueOf(userId)).resolve(subdir).normalize();

        if (!dir.startsWith(baseDir)) {
            throw new SecurityException("Chemin non autorisé.");
        }
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    public Photo savePhoto(MultipartFile file, Worker worker) throws IOException {
        validateImage(file);

        String uuid = UUID.randomUUID().toString();
        String ext = getExtension(file.getOriginalFilename());
        String baseName = uuid + ext;

        Path originalsDir = resolveDir(worker.getId(), "originals");
        Path mainThumbDir = resolveDir(worker.getId(), "thumbs/main");

        Path originalPath = originalsDir.resolve(baseName).normalize();
        Path mainThumbPath = mainThumbDir.resolve(uuid + "_main" + ext).normalize();

        Files.write(originalPath, file.getBytes());

        Thumbnails.of(originalPath.toFile())
                .size(MAIN_THUMB_W, MAIN_THUMB_H)
                .crop(Positions.CENTER)
                .outputQuality(0.85)
                .toFile(mainThumbPath.toFile());

        Photo photo = new Photo();
        photo.setUrl(buildUrl(worker.getId(), "originals", baseName));
        photo.setMainThumbUrl(buildUrl(worker.getId(), "thumbs/main", uuid + "_main" + ext));
        photo.setFileSize(file.getSize());
        photo.setWorker(worker);

        return photo;
    }

    // SUPPRESSION TOTALE ULTRA-RAPIDE : Supprime tout le dossier /{uploadBase}/{userId} en une seule fois !
    public void deleteAllForUser(UUID userId) throws IOException {
        Path baseDir = Paths.get(uploadBase).toAbsolutePath().normalize();
        Path userDir = baseDir.resolve(String.valueOf(userId)).normalize();

        if (userDir.startsWith(baseDir) && Files.exists(userDir)) {
            Files.walk(userDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
    }

    private String buildUrl(UUID userId, String subdir, String filename) {
        return publicBaseUrl + "/" + userId + "/" + subdir + "/" + filename;
    }

    public void deletePhotoFiles(UUID workerId, Photo photo) {
        try {
            deletePhysicalFile("originals", workerId, photo.getUrl());
            deletePhysicalFile("thumbs/main", workerId, photo.getMainThumbUrl());
        } catch (IOException e) {
            System.err.println("Erreur suppression physique pour le worker " + workerId + ": " + e.getMessage());
        }
    }

    private void deletePhysicalFile(String subdir, UUID workerId, String url) throws IOException {
        if (url == null || !url.contains("/")) return;

        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path baseDir = Paths.get(uploadBase).toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(subdir).resolve(String.valueOf(workerId)).normalize();
        Path filePath = targetDir.resolve(filename).normalize();

        if (!filePath.startsWith(targetDir)) {
            throw new SecurityException("Tentative de Path Traversal détectée !");
        }

        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        if (Files.isDirectory(targetDir)) {
            try (var entries = Files.newDirectoryStream(targetDir)) {
                if (!entries.iterator().hasNext()) {
                    Files.delete(targetDir);
                }
            }
        }
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

    /**
     * Sauvegarde une vidéo et retourne une entité Video prête à l'emploi.
     */
    public Video saveVideo(MultipartFile file, Worker worker) throws IOException {
        validateVideo(file);

        String uuid = UUID.randomUUID().toString();
        String ext = getExtension(file.getOriginalFilename());
        String baseName = uuid + ext;

        // On sauvegarde les vidéos dans le dossier "videos"
        Path videosDir = resolveDir(worker.getId(),"videos");
        Path videoPath = videosDir.resolve(baseName).normalize();

        // 1 — Sauvegarde physique de la vidéo
        Files.write(videoPath, file.getBytes());

        // 2 — Instanciation et remplissage de l'entité Video
        Video video = new Video();
        video.setUrl(buildUrl(worker.getId(),"videos",  uuid));
        video.setFileSize(file.getSize());
        video.setWorker(worker);

        return video;
    }

    private void validateVideo(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("video/")) {
            throw new IllegalArgumentException("Only video files are accepted.");
        }
        // Par exemple, limite à 50 Mo pour les vidéos (à adapter selon tes besoins)
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("Video must be smaller than 50 MB.");
        }
    }

    public void deleteVideo(UUID workerId, Video video) {
        try {
            deletePhysicalFile("videos", workerId, video.getUrl());
        } catch (IOException e) {
            System.err.println("Erreur suppression physique de la vidéo pour le worker " + workerId + ": " + e.getMessage());
        }
    }
}