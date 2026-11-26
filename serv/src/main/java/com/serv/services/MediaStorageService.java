package com.serv.services;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Video;
import com.serv.database.entities.Worker;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Handles all media storage.
 * <p>
 * Uploaded files are never trusted: their real type is detected from their content (magic bytes),
 * the stored extension is derived from that type (never from the client file name), and images are
 * fully decoded and re-encoded as JPEG. Re-encoding removes any embedded payload and all metadata,
 * including EXIF GPS coordinates that could reveal where a photo was taken.
 * <p>
 * Directory layout on disk:
 * <pre>
 *   ${media.upload.base}/{workerId}/originals/{uuid}.jpg
 *   ${media.upload.base}/{workerId}/thumbs/main/{uuid}_main.jpg
 *   ${media.private.base}/{workerId}/certification/{uuid}.jpg   (never served by nginx)
 *   ${media.upload.base}/{workerId}/videos/{uuid}.mp4                (re-encoded, see VideoProcessingService)
 *   ${media.upload.base}/{workerId}/thumbs/videos/{uuid}.jpg
 *   ${media.private.base}/{workerId}/video-uploads/{uuid}.upload      (raw upload, deleted once processed)
 * </pre>
 * Nginx serves everything under ${media.upload.base} directly. Certification photos (face + code)
 * live under ${media.private.base}, outside the public directory, and are only readable by admins
 * through {@code GET /admin/certification-photos/{workerId}/{file}}.
 */
@Slf4j
@Service
public class MediaStorageService {

    public static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    /** Raw upload limit (phones record large files); the stored, re-encoded video is much smaller. */
    public static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    /** Rejects decompression bombs: a tiny file declaring a huge canvas. */
    public static final int MAX_IMAGE_SIDE = 10_000;
    public static final long MAX_IMAGE_PIXELS = 40_000_000L;
    /** Stored originals are downscaled to this size (longest side). */
    public static final int MAX_STORED_SIDE = 2560;

    private static final int MAIN_THUMB_W = 600;
    private static final int MAIN_THUMB_H = 800;
    private static final double JPEG_QUALITY = 0.88;

    /** Image decoding is memory hungry: limit how many run at the same time. */
    private final Semaphore imageProcessing = new Semaphore(2);

    @Value("${media.upload.base}")
    private String uploadBase;

    @Value("${media.public.base-url}")
    private String publicBaseUrl;

    @Value("${media.private.base:${media.upload.base}-private}")
    private String privateBase;

    /** API path prefix stored as the URL of certification photos. */
    public static final String CERTIFICATION_URL_PREFIX = "/admin/certification-photos/";
    private static final java.util.regex.Pattern PRIVATE_FILE_NAME =
            java.util.regex.Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$");

    enum ImageType { JPEG, PNG, WEBP, GIF }

    enum VideoType {
        MP4(".mp4"), QUICKTIME(".mov"), WEBM(".webm");
        final String extension;
        VideoType(String extension) { this.extension = extension; }
    }

    // ── Photos ────────────────────────────────────────────────────────────────

    public Photo savePhoto(MultipartFile file, Worker worker) throws IOException {
        BufferedImage image = readValidatedImage(file);

        String uuid = UUID.randomUUID().toString();
        Path originalPath = resolveDir(worker.getId(), "originals").resolve(uuid + ".jpg");
        Path mainThumbPath = resolveDir(worker.getId(), "thumbs/main").resolve(uuid + "_main.jpg");

        try {
            writeJpeg(image, originalPath, MAX_STORED_SIDE);
            Thumbnails.of(image)
                    .size(MAIN_THUMB_W, MAIN_THUMB_H)
                    .crop(Positions.CENTER)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toFile(mainThumbPath.toFile());
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(originalPath);
            Files.deleteIfExists(mainThumbPath);
            throw e;
        }

        Photo photo = new Photo();
        photo.setUrl(buildUrl(worker.getId(), "originals", uuid + ".jpg"));
        photo.setMainThumbUrl(buildUrl(worker.getId(), "thumbs/main", uuid + "_main.jpg"));
        photo.setFileSize(Files.size(originalPath) + Files.size(mainThumbPath));
        photo.setWorker(worker);
        return photo;
    }

    public Photo saveCertificationPhoto(MultipartFile file, Worker worker) throws IOException {
        BufferedImage image = readValidatedImage(file);

        String baseName = UUID.randomUUID() + ".jpg";
        Path certPath = resolvePrivateCertificationDir(worker.getId()).resolve(baseName);
        writeJpeg(image, certPath, MAX_STORED_SIDE);

        Photo photo = new Photo();
        photo.setUrl(CERTIFICATION_URL_PREFIX + worker.getId() + "/" + baseName);
        photo.setFileSize(Files.size(certPath));
        photo.setWorker(worker);
        return photo;
    }

    /**
     * Validates the declared type, the real type (magic bytes), the size and the dimensions,
     * then decodes the image.
     */
    BufferedImage readValidatedImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file.");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are accepted.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image must be smaller than 20 MB.");
        }

        byte[] bytes = file.getBytes();
        if (detectImageType(bytes) == null) {
            throw new IllegalArgumentException("Unsupported image format.");
        }

        boolean acquired = false;
        try {
            imageProcessing.acquire();
            acquired = true;
            return decodeImage(bytes);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while processing image.", e);
        } finally {
            if (acquired) imageProcessing.release();
        }
    }

    private static BufferedImage decodeImage(byte[] bytes) throws IOException {
        // 1. Read the declared dimensions without decoding the pixels.
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Unsupported image format.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_SIDE || height > MAX_IMAGE_SIDE
                        || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Image dimensions are too large.");
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Unreadable image.");
            } finally {
                reader.dispose();
            }
        }

        // 2. Decode. Thumbnailator applies the EXIF orientation, so phone pictures are not stored rotated.
        try {
            BufferedImage image = Thumbnails.of(new ByteArrayInputStream(bytes)).scale(1.0).asBufferedImage();
            return toRgb(image);
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("Unreadable image.");
        }
    }

    /** JPEG has no alpha channel: flatten transparent images onto a white background. */
    private static BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) return source;
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, source.getWidth(), source.getHeight());
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static void writeJpeg(BufferedImage image, Path target, int maxSide) throws IOException {
        var builder = Thumbnails.of(image);
        if (image.getWidth() > maxSide || image.getHeight() > maxSide) {
            builder = Thumbnails.of(image).size(maxSide, maxSide);
        } else {
            builder = builder.scale(1.0);
        }
        builder.outputFormat("jpg").outputQuality(JPEG_QUALITY).toFile(target.toFile());
    }

    static ImageType detectImageType(byte[] b) {
        if (b == null || b.length < 12) return null;
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return ImageType.JPEG;
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return ImageType.PNG;
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return ImageType.WEBP;
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return ImageType.GIF;
        return null;
    }

    // ── Videos ────────────────────────────────────────────────────────────────

    /**
     * Keeps the raw upload outside the public directory and returns a video waiting to be processed:
     * its public URL and thumbnail URL are known, the files appear there once re-encoded.
     */
    public Video saveVideo(MultipartFile file, Worker worker) throws IOException {
        validateVideo(file);

        String uuid = UUID.randomUUID().toString();
        Path pending = resolvePrivateDir(worker.getId(), "video-uploads").resolve(uuid + ".upload");

        // Streamed to disk: a large video is never loaded in memory.
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, pending, StandardCopyOption.REPLACE_EXISTING);
        }

        Video video = new Video();
        video.setUrl(buildUrl(worker.getId(), "videos", uuid + ".mp4"));
        video.setMainThumbUrl(buildUrl(worker.getId(), "thumbs/videos", uuid + ".jpg"));
        video.setStatus(com.serv.common.VideoStatus.PROCESSING);
        video.setFileSize(0L);
        video.setWorker(worker);
        return video;
    }

    /** Raw upload of a video waiting to be processed. */
    public Path pendingVideoFile(UUID workerId, Video video) throws IOException {
        return resolvePrivateDir(workerId, "video-uploads").resolve(fileStem(video.getUrl()) + ".upload");
    }

    /** Where the re-encoded video is written before being moved in place (not public). */
    public Path encodingVideoFile(UUID workerId, Video video) throws IOException {
        return resolvePrivateDir(workerId, "video-uploads").resolve(fileStem(video.getUrl()) + ".encoding.mp4");
    }

    public Path publicVideoFile(UUID workerId, Video video) throws IOException {
        return resolveDir(workerId, "videos").resolve(fileStem(video.getUrl()) + ".mp4");
    }

    public Path publicVideoThumbnail(UUID workerId, Video video) throws IOException {
        return resolveDir(workerId, "thumbs/videos").resolve(fileStem(video.getUrl()) + ".jpg");
    }

    /** UUID part of a stored media URL; anything else is refused (the name is used to build paths). */
    private static String fileStem(String url) {
        String name = url == null ? "" : url.substring(url.lastIndexOf('/') + 1);
        int dot = name.indexOf('.');
        String stem = dot < 0 ? name : name.substring(0, dot);
        if (!stem.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            throw new SecurityException("Unexpected media file name.");
        }
        return stem;
    }

    private Path resolvePrivateDir(UUID userId, String subdir) throws IOException {
        Path base = privateBaseDir();
        Path dir = base.resolve(String.valueOf(userId)).resolve(subdir).normalize();
        if (!dir.startsWith(base)) {
            throw new SecurityException("Path not allowed.");
        }
        Files.createDirectories(dir);
        return dir;
    }

    private VideoType validateVideo(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file.");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("video/")) {
            throw new IllegalArgumentException("Only video files are accepted.");
        }
        if (file.getSize() > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("Video must be smaller than 100 MB.");
        }
        byte[] header;
        try (InputStream in = file.getInputStream()) {
            header = in.readNBytes(16);
        }
        VideoType type = detectVideoType(header);
        if (type == null) {
            throw new IllegalArgumentException("Unsupported video format.");
        }
        return type;
    }

    static VideoType detectVideoType(byte[] b) {
        if (b == null || b.length < 12) return null;
        if ((b[0] & 0xFF) == 0x1A && (b[1] & 0xFF) == 0x45 && (b[2] & 0xFF) == 0xDF && (b[3] & 0xFF) == 0xA3) {
            return VideoType.WEBM;
        }
        if (b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            String brand = new String(Arrays.copyOfRange(b, 8, 12), java.nio.charset.StandardCharsets.US_ASCII);
            return brand.startsWith("qt") ? VideoType.QUICKTIME : VideoType.MP4;
        }
        return null;
    }

    // ── Deletion ──────────────────────────────────────────────────────────────

    /** Deletes the whole /{uploadBase}/{userId} directory. */
    public void deleteAllForUser(UUID userId) throws IOException {
        deleteUserDir(baseDir(), userId);
        deleteUserDir(privateBaseDir(), userId);
    }

    private static void deleteUserDir(Path baseDir, UUID userId) throws IOException {
        Path userDir = baseDir.resolve(String.valueOf(userId)).normalize();

        if (!userDir.startsWith(baseDir) || userDir.equals(baseDir) || !Files.exists(userDir)) {
            return;
        }
        try (var paths = Files.walk(userDir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);
        }
    }

    public void deletePhotoFiles(UUID workerId, Photo photo) {
        try {
            deletePhysicalFile(workerId, "originals", photo.getUrl());
            deletePhysicalFile(workerId, "thumbs/main", photo.getMainThumbUrl());
            deletePrivateCertificationFile(workerId, photo.getUrl());
        } catch (IOException e) {
            log.warn("Could not delete photo files of worker {}: {}", workerId, e.getMessage());
        }
    }

    public void deleteVideo(UUID workerId, Video video) {
        try {
            deletePhysicalFile(workerId, "videos", video.getUrl());
            deletePhysicalFile(workerId, "thumbs/videos", video.getMainThumbUrl());
            Files.deleteIfExists(pendingVideoFile(workerId, video));
            Files.deleteIfExists(encodingVideoFile(workerId, video));
        } catch (IOException | SecurityException e) {
            log.warn("Could not delete video file of worker {}: {}", workerId, e.getMessage());
        }
    }

    /** Deletes {@code /{uploadBase}/{workerId}/{subdir}/{file name taken from url}} if it exists. */
    private void deletePhysicalFile(UUID workerId, String subdir, String url) throws IOException {
        if (url == null || !url.contains("/" + subdir + "/")) return;

        String filename = url.substring(url.lastIndexOf('/') + 1);
        if (filename.isEmpty() || filename.contains("..")) return;

        Path targetDir = baseDir().resolve(String.valueOf(workerId)).resolve(subdir).normalize();
        Path filePath = targetDir.resolve(filename).normalize();

        if (!filePath.startsWith(targetDir) || !filePath.getParent().equals(targetDir)) {
            throw new SecurityException("Path traversal attempt detected.");
        }

        Files.deleteIfExists(filePath);
    }

    // ── Private certification photos ──────────────────────────────────────────

    /**
     * Resolves a certification photo for an admin. Returns empty for anything that is not a
     * well-formed file name of that worker (no traversal, no other directory).
     */
    public java.util.Optional<Path> findCertificationPhoto(UUID workerId, String fileName) {
        if (fileName == null || !PRIVATE_FILE_NAME.matcher(fileName).matches()) {
            return java.util.Optional.empty();
        }
        Path dir = privateBaseDir().resolve(workerId.toString()).resolve("certification").normalize();
        Path file = dir.resolve(fileName).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(file);
    }

    /**
     * Moves a certification photo stored by an older version in the public media directory
     * to the private directory. @return the new URL, or empty if there was nothing to migrate
     */
    public java.util.Optional<String> migrateLegacyCertificationPhoto(UUID workerId, String url) throws IOException {
        String legacyMarker = "/" + workerId + "/certification/";
        if (url == null || url.startsWith(CERTIFICATION_URL_PREFIX) || !url.contains(legacyMarker)) {
            return java.util.Optional.empty();
        }
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        Path legacyDir = baseDir().resolve(workerId.toString()).resolve("certification").normalize();
        Path legacy = legacyDir.resolve(fileName).normalize();
        if (!legacy.startsWith(legacyDir)) {
            return java.util.Optional.empty();
        }
        String newName = UUID.randomUUID() + ".jpg";
        if (Files.isRegularFile(legacy)) {
            // Re-encode on the way: legacy files were stored as uploaded, possibly with metadata.
            BufferedImage image;
            try {
                image = decodeImage(Files.readAllBytes(legacy));
            } catch (IllegalArgumentException e) {
                Files.deleteIfExists(legacy); // not a valid image: never keep it public
                return java.util.Optional.of(CERTIFICATION_URL_PREFIX + workerId + "/missing.jpg");
            }
            writeJpeg(image, resolvePrivateCertificationDir(workerId).resolve(newName), MAX_STORED_SIDE);
            Files.delete(legacy);
        }
        return java.util.Optional.of(CERTIFICATION_URL_PREFIX + workerId + "/" + newName);
    }

    private void deletePrivateCertificationFile(UUID workerId, String url) throws IOException {
        if (url == null || !url.startsWith(CERTIFICATION_URL_PREFIX)) return;
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        java.util.Optional<Path> file = findCertificationPhoto(workerId, fileName);
        if (file.isPresent()) Files.delete(file.get());
    }

    private Path resolvePrivateCertificationDir(UUID workerId) throws IOException {
        Path base = privateBaseDir();
        Path dir = base.resolve(workerId.toString()).resolve("certification").normalize();
        if (!dir.startsWith(base)) {
            throw new SecurityException("Path not allowed.");
        }
        Files.createDirectories(dir);
        return dir;
    }

    private Path privateBaseDir() {
        return Paths.get(privateBase).toAbsolutePath().normalize();
    }

    // ── Paths ─────────────────────────────────────────────────────────────────

    /**
     * Storage currently used by a worker, computed from the files on disk.
     */
    public long usedBytes(UUID workerId) {
        Path userDir = baseDir().resolve(String.valueOf(workerId)).normalize();
        if (!Files.isDirectory(userDir)) return 0L;
        try (var paths = Files.walk(userDir)) {
            return paths.filter(Files::isRegularFile).mapToLong(p -> {
                try { return Files.size(p); } catch (IOException e) { return 0L; }
            }).sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private Path baseDir() {
        return Paths.get(uploadBase).toAbsolutePath().normalize();
    }

    private Path resolveDir(UUID userId, String subdir) throws IOException {
        Path baseDir = baseDir();
        Path dir = baseDir.resolve(String.valueOf(userId)).resolve(subdir).normalize();

        if (!dir.startsWith(baseDir)) {
            throw new SecurityException("Path not allowed.");
        }
        Files.createDirectories(dir);
        return dir;
    }

    private String buildUrl(UUID userId, String subdir, String filename) {
        return publicBaseUrl + "/" + userId + "/" + subdir + "/" + filename;
    }
}
