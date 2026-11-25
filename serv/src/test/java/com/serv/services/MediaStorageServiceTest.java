package com.serv.services;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Video;
import com.serv.database.entities.Worker;
import com.serv.support.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaStorageServiceTest {

    private MediaStorageService service;

    @TempDir
    Path tempDir;

    private Worker worker;

    @BeforeEach
    void setUp() {
        service = new MediaStorageService();
        ReflectionTestUtils.setField(service, "uploadBase", tempDir.toString());
        ReflectionTestUtils.setField(service, "publicBaseUrl", "/media");

        worker = new Worker();
        worker.setId(UUID.randomUUID());
    }

    private Path workerDir() {
        return tempDir.resolve(worker.getId().toString());
    }

    @Test
    @DisplayName("savePhoto stores a JPEG original and a thumbnail under the worker directory")
    void savePhoto() throws IOException {
        Photo photo = service.savePhoto(new MockMultipartFile("f", "a.png", "image/png", TestImages.png(640, 480)), worker);

        assertThat(photo.getUrl()).startsWith("/media/" + worker.getId() + "/originals/").endsWith(".jpg");
        assertThat(photo.getMainThumbUrl()).startsWith("/media/" + worker.getId() + "/thumbs/main/").endsWith("_main.jpg");
        assertThat(photo.getFileSize()).isPositive();
        assertThat(Files.list(workerDir().resolve("originals"))).hasSize(1);
        assertThat(Files.list(workerDir().resolve("thumbs/main"))).hasSize(1);
    }

    @Test
    @DisplayName("savePhoto rejects non-image content types")
    void savePhoto_wrongType() {
        assertThatThrownBy(() -> service.savePhoto(new MockMultipartFile("f", "a.txt", "text/plain", "hi".getBytes()), worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only image files are accepted.");
    }

    @Test
    @DisplayName("savePhoto rejects content that is not really an image")
    void savePhoto_fakeImage() {
        assertThatThrownBy(() -> service.savePhoto(new MockMultipartFile("f", "a.jpg", "image/jpeg", TestImages.htmlPayload()), worker))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(workerDir()).doesNotExist();
    }

    @Test
    @DisplayName("savePhoto rejects images above 20 MB")
    void savePhoto_tooBig() {
        byte[] big = new byte[(int) MediaStorageService.MAX_IMAGE_BYTES + 1];
        System.arraycopy(TestImages.jpeg(10, 10), 0, big, 0, 20);
        assertThatThrownBy(() -> service.savePhoto(new MockMultipartFile("f", "a.jpg", "image/jpeg", big), worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20 MB");
    }

    @Test
    @DisplayName("saveVideo stores a recognised video with the extension of its real format")
    void saveVideo() throws IOException {
        Video video = service.saveVideo(new MockMultipartFile("f", "clip.exe", "video/mp4", TestImages.mp4()), worker);
        assertThat(video.getUrl()).endsWith(".mp4");
        assertThat(video.getFileSize()).isEqualTo(TestImages.mp4().length);
    }

    @Test
    @DisplayName("saveVideo rejects unknown formats and non-video types")
    void saveVideo_invalid() {
        assertThatThrownBy(() -> service.saveVideo(new MockMultipartFile("f", "a.mp4", "video/mp4", new byte[1024]), worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported video format.");
        assertThatThrownBy(() -> service.saveVideo(new MockMultipartFile("f", "a.jpg", "image/jpeg", TestImages.jpeg(5, 5)), worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only video files are accepted.");
    }

    @Test
    @DisplayName("deletePhotoFiles removes both the original and the thumbnail")
    void deletePhotoFiles() throws IOException {
        Photo photo = service.savePhoto(new MockMultipartFile("f", "a.jpg", "image/jpeg", TestImages.jpeg(100, 100)), worker);
        service.deletePhotoFiles(worker.getId(), photo);
        assertThat(Files.list(workerDir().resolve("originals"))).isEmpty();
        assertThat(Files.list(workerDir().resolve("thumbs/main"))).isEmpty();
    }

    @Test
    @DisplayName("deletePhotoFiles ignores URLs trying to escape the worker directory")
    void deletePhotoFiles_traversal() throws IOException {
        Path victim = Files.writeString(tempDir.resolve("victim.txt"), "keep me");
        Photo photo = new Photo();
        photo.setUrl("/media/" + worker.getId() + "/originals/../../victim.txt");
        service.deletePhotoFiles(worker.getId(), photo);
        assertThat(victim).exists();
    }

    @Test
    @DisplayName("deleteAllForUser removes the whole worker directory and nothing else")
    void deleteAllForUser() throws IOException {
        service.savePhoto(new MockMultipartFile("f", "a.jpg", "image/jpeg", TestImages.jpeg(50, 50)), worker);
        Path neighbour = Files.writeString(tempDir.resolve("other.txt"), "x");

        service.deleteAllForUser(worker.getId());

        assertThat(workerDir()).doesNotExist();
        assertThat(neighbour).exists();
    }

    @Test
    @DisplayName("usedBytes sums the worker's files on disk")
    void usedBytes() throws IOException {
        assertThat(service.usedBytes(worker.getId())).isZero();
        Photo photo = service.savePhoto(new MockMultipartFile("f", "a.jpg", "image/jpeg", TestImages.jpeg(200, 200)), worker);
        assertThat(service.usedBytes(worker.getId())).isEqualTo(photo.getFileSize());
    }

    @Test
    @DisplayName("Magic-byte detection")
    void detection() {
        assertThat(MediaStorageService.detectImageType(TestImages.jpeg(5, 5))).isEqualTo(MediaStorageService.ImageType.JPEG);
        assertThat(MediaStorageService.detectImageType(TestImages.png(5, 5))).isEqualTo(MediaStorageService.ImageType.PNG);
        assertThat(MediaStorageService.detectImageType(TestImages.htmlPayload())).isNull();
        assertThat(MediaStorageService.detectImageType(new byte[3])).isNull();
        assertThat(MediaStorageService.detectVideoType(TestImages.mp4())).isEqualTo(MediaStorageService.VideoType.MP4);
        assertThat(MediaStorageService.detectVideoType(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0, 0, 0, 0, 0, 0, 0, 0}))
                .isEqualTo(MediaStorageService.VideoType.WEBM);
    }
}
