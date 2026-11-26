package com.serv.security;

import com.serv.database.entities.Photo;
import com.serv.common.PhotoType;
import com.serv.database.entities.Worker;
import com.serv.support.IntegrationTest;
import com.serv.support.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MediaUploadSecurityTest extends IntegrationTest {

    private Worker worker;

    @BeforeEach
    void setUp() {
        worker = createWorker("worker@venus.test");
    }

    private ResultActions upload(MockMultipartFile... files) throws Exception {
        var req = multipart("/account/worker/media");
        for (MockMultipartFile f : files) req.file(f);
        return mvc.perform(req.header("Authorization", bearer(worker)));
    }

    private static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("files", name, contentType, content);
    }

    private List<Path> storedFiles() throws IOException {
        Path dir = MEDIA_DIR.resolve(worker.getId().toString());
        if (!Files.exists(dir)) return List.of();
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile).toList();
        }
    }

    @Test
    @DisplayName("A real JPEG is accepted and stored as .jpg with a thumbnail")
    void validJpeg() throws Exception {
        upload(file("me.jpg", "image/jpeg", TestImages.jpeg(800, 600))).andExpect(status().isOk());

        List<Path> files = storedFiles();
        assertThat(files).hasSize(2);
        assertThat(files).allMatch(p -> p.getFileName().toString().endsWith(".jpg"));
        assertThat(photoRepository.findByWorker(worker)).hasSize(1);

        // Each photo reports its weight on disk (original and thumbnail), for the storage choices
        long onDisk = 0;
        for (Path f : files) onDisk += Files.size(f);
        mvc.perform(get("/account/worker/me").header("Authorization", bearer(worker)))
                .andExpect(jsonPath("$.photos[0].fileSize").value(onDisk));
    }

    @Test
    @DisplayName("The storage endpoint reports the bytes on disk and the limit of the worker only")
    void storageUsage() throws Exception {
        mvc.perform(get("/account/worker/storage").header("Authorization", bearer(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedBytes").value(0))
                .andExpect(jsonPath("$.maxBytes").value(50L * 1024 * 1024)); // default STORAGE_LIMIT_MB

        upload(file("me.jpg", "image/jpeg", TestImages.jpeg(800, 600))).andExpect(status().isOk());
        long onDisk = 0;
        for (Path f : storedFiles()) onDisk += Files.size(f);

        mvc.perform(get("/account/worker/storage").header("Authorization", bearer(worker)))
                .andExpect(jsonPath("$.usedBytes").value(onDisk));

        Worker other = createWorker("other@venus.test");
        mvc.perform(get("/account/worker/storage").header("Authorization", bearer(other)))
                .andExpect(jsonPath("$.usedBytes").value(0));
    }

    @Test
    @DisplayName("PNG uploads are re-encoded as JPEG")
    void pngReencoded() throws Exception {
        upload(file("me.png", "image/png", TestImages.png(300, 300))).andExpect(status().isOk());
        for (Path p : storedFiles()) {
            byte[] bytes = Files.readAllBytes(p);
            assertThat(bytes[0] & 0xFF).isEqualTo(0xFF);
            assertThat(bytes[1] & 0xFF).isEqualTo(0xD8);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"evil.html", "evil.svg", "evil.php", "evil.jsp", "evil.jpg.html", "../../evil.jpg", "evil"})
    @DisplayName("The client file name never decides the stored extension or path")
    void fileNameIgnored(String name) throws Exception {
        upload(file(name, "image/jpeg", TestImages.jpeg(100, 100))).andExpect(status().isOk());
        assertThat(storedFiles()).allMatch(p -> p.getFileName().toString().endsWith(".jpg"));
        assertThat(storedFiles()).allMatch(p -> p.startsWith(MEDIA_DIR.resolve(worker.getId().toString())));
    }

    @Test
    @DisplayName("HTML disguised as an image is rejected and nothing is written to disk")
    void htmlDisguisedAsImage() throws Exception {
        upload(file("cat.jpg", "image/jpeg", TestImages.htmlPayload())).andExpect(status().isBadRequest());
        assertThat(storedFiles()).isEmpty();
        assertThat(photoRepository.count()).isZero();
    }

    @Test
    @DisplayName("SVG (which can embed scripts) is rejected")
    void svgRejected() throws Exception {
        byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg'><script>alert(1)</script></svg>".getBytes();
        upload(file("a.svg", "image/svg+xml", svg)).andExpect(status().isBadRequest());
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("A truncated / corrupted image is rejected")
    void corruptedImage() throws Exception {
        upload(file("broken.jpg", "image/jpeg", TestImages.truncatedJpeg())).andExpect(status().isBadRequest());
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("A payload appended to a valid JPEG does not survive re-encoding")
    void polyglotSanitized() throws Exception {
        upload(file("poly.jpg", "image/jpeg", TestImages.polyglotJpeg())).andExpect(status().isOk());
        for (Path p : storedFiles()) {
            String content = new String(Files.readAllBytes(p), java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(content).doesNotContain("<script>").doesNotContain("<?php");
        }
    }

    @Test
    @DisplayName("Decompression bombs (huge declared dimensions) are rejected")
    void decompressionBomb() throws Exception {
        upload(file("bomb.png", "image/png", TestImages.pngDeclaring(60_000, 60_000))).andExpect(status().isBadRequest());
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("Large originals are downscaled before storage")
    void largeImageDownscaled() throws Exception {
        upload(file("big.jpg", "image/jpeg", TestImages.jpeg(4000, 3000))).andExpect(status().isOk());
        Path original = storedFiles().stream().filter(p -> p.toString().contains("originals")).findFirst().orElseThrow();
        var img = ImageIO.read(new ByteArrayInputStream(Files.readAllBytes(original)));
        assertThat(Math.max(img.getWidth(), img.getHeight())).isLessThanOrEqualTo(2560);
    }

    @Test
    @DisplayName("Content types other than image/video are refused")
    void otherContentTypes() throws Exception {
        upload(file("doc.pdf", "application/pdf", "%PDF-1.4".getBytes())).andExpect(status().isUnsupportedMediaType());
        upload(file("x.bin", "application/octet-stream", TestImages.jpeg(10, 10))).andExpect(status().isUnsupportedMediaType());
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("A fake video (wrong magic bytes) is rejected")
    void fakeVideo() throws Exception {
        upload(file("clip.mp4", "video/mp4", TestImages.htmlPayload())).andExpect(status().isBadRequest());
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("An uploaded video never reaches the public folder as uploaded (it waits to be re-encoded)")
    void videoNotPublishedRaw() throws Exception {
        org.mockito.Mockito.when(videoTranscoder.probe(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IOException("not a video"));
        upload(file("clip.html", "video/mp4", TestImages.mp4())).andExpect(status().isOk());
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("Too many files in one request are refused")
    void tooManyFiles() throws Exception {
        MockMultipartFile[] files = new MockMultipartFile[11];
        for (int i = 0; i < files.length; i++) files[i] = file("p" + i + ".jpg", "image/jpeg", TestImages.jpeg(10, 10));
        upload(files).andExpect(status().isBadRequest());
        assertThat(storedFiles()).isEmpty();
    }

    @Test
    @DisplayName("An empty upload is refused")
    void emptyFile() throws Exception {
        upload(file("empty.jpg", "image/jpeg", new byte[0])).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("The storage quota is enforced")
    void quota() throws Exception {
        Path dir = Files.createDirectories(MEDIA_DIR.resolve(worker.getId().toString()).resolve("originals"));
        Files.write(dir.resolve("filler.jpg"), new byte[50 * 1024 * 1024] /* default STORAGE_LIMIT_MB */);
        upload(file("one-more.jpg", "image/jpeg", TestImages.jpeg(50, 50))).andExpect(status().isContentTooLarge());
    }

    @Test
    @DisplayName("Deleting a photo removes its files from disk")
    void deleteRemovesFiles() throws Exception {
        upload(file("me.jpg", "image/jpeg", TestImages.jpeg(200, 200))).andExpect(status().isOk());
        Photo photo = photoRepository.findByWorker(worker).get(0);
        assertThat(storedFiles()).hasSize(2);

        mvc.perform(delete("/account/worker/photos/" + photo.getId()).header("Authorization", bearer(worker)))
                .andExpect(status().isOk());

        assertThat(storedFiles()).isEmpty();
        assertThat(photoRepository.findById(photo.getId())).isEmpty();
    }

    @Test
    @DisplayName("A worker cannot delete another worker's photo")
    void cannotDeleteOthersPhoto() throws Exception {
        Worker other = createVisibleWorker("other@venus.test");
        Photo foreign = photoRepository.findByWorker(other).get(0);
        mvc.perform(delete("/account/worker/photos/" + foreign.getId()).header("Authorization", bearer(worker)))
                .andExpect(status().isNotFound());
        assertThat(photoRepository.findById(foreign.getId())).isPresent();
    }

    @Test
    @DisplayName("Certification photos are validated like gallery photos")
    void certificationPhotoValidated() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/account/worker/request-certification")
                .header("Authorization", bearer(worker))).andExpect(status().isOk());

        mvc.perform(multipart("/account/worker/certification-photo")
                        .file(new MockMultipartFile("file", "id.jpg", "image/jpeg", TestImages.htmlPayload()))
                        .header("Authorization", bearer(worker)))
                .andExpect(status().isBadRequest());
        assertThat(storedFiles()).isEmpty();

        mvc.perform(multipart("/account/worker/certification-photo")
                        .file(new MockMultipartFile("file", "id.html", "image/jpeg", TestImages.jpeg(300, 300)))
                        .header("Authorization", bearer(worker)))
                .andExpect(status().isOk());
        assertThat(photoRepository.findAll()).singleElement().matches(p -> p.getType() == PhotoType.CERTIFICATION);
    }

    @Test
    @DisplayName("Certification photos are stored outside the public media directory and only admins can read them")
    void certificationPhotoIsPrivate() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/account/worker/request-certification")
                .header("Authorization", bearer(worker))).andExpect(status().isOk());
        mvc.perform(multipart("/account/worker/certification-photo")
                        .file(new MockMultipartFile("file", "id.jpg", "image/jpeg", TestImages.jpeg(300, 300)))
                        .header("Authorization", bearer(worker)))
                .andExpect(status().isOk());

        assertThat(storedFiles()).as("nothing under the nginx-served directory").isEmpty();
        Photo cert = photoRepository.findAll().get(0);
        assertThat(cert.getUrl()).startsWith("/admin/certification-photos/" + worker.getId() + "/");
        try (Stream<Path> s = Files.walk(PRIVATE_MEDIA_DIR)) {
            assertThat(s.filter(Files::isRegularFile).count()).isEqualTo(1);
        }

        var get = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(cert.getUrl());
        mvc.perform(get).andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(cert.getUrl())
                .header("Authorization", bearer(worker))).andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(cert.getUrl())
                        .header("Authorization", bearer(createAdmin("admin@venus.test"))))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/jpeg"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    @Test
    @DisplayName("The certification photo endpoint cannot be used to read arbitrary files")
    void certificationEndpointTraversal() throws Exception {
        String admin = bearer(createAdmin("admin@venus.test"));
        Files.writeString(PRIVATE_MEDIA_DIR.resolve("secret.txt"), "secret");
        for (String path : new String[]{
                "/admin/certification-photos/" + worker.getId() + "/..%2F..%2Fsecret.txt",
                "/admin/certification-photos/" + worker.getId() + "/secret.txt",
                "/admin/certification-photos/" + worker.getId() + "/" + java.util.UUID.randomUUID() + ".jpg",
                "/admin/certification-photos/not-a-uuid/x.jpg"}) {
            int code = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                    .header("Authorization", admin)).andReturn().getResponse().getStatus();
            assertThat(code).as(path).isIn(400, 404);
        }
    }

    @Test
    @DisplayName("Legacy public certification photos are moved to private storage at startup")
    void legacyMigration(@org.springframework.beans.factory.annotation.Autowired com.serv.services.CertificationPhotoMigration migration) throws Exception {
        Path legacyDir = Files.createDirectories(MEDIA_DIR.resolve(worker.getId().toString()).resolve("certification"));
        Files.write(legacyDir.resolve("old.jpg"), TestImages.jpeg(100, 100));
        Photo legacy = new Photo();
        legacy.setWorker(worker);
        legacy.setType(PhotoType.CERTIFICATION);
        legacy.setUrl("/media/" + worker.getId() + "/certification/old.jpg");
        legacy = photoRepository.save(legacy);

        migration.run(null);

        assertThat(legacyDir.resolve("old.jpg")).doesNotExist();
        String url = photoRepository.findById(legacy.getId()).orElseThrow().getUrl();
        assertThat(url).startsWith("/admin/certification-photos/");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header("Authorization", bearer(createAdmin("admin@venus.test")))).andExpect(status().isOk());

        migration.run(null); // idempotent
        assertThat(photoRepository.findById(legacy.getId()).orElseThrow().getUrl()).isEqualTo(url);
    }
}
