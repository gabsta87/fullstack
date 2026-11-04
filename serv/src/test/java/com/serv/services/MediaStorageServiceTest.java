package com.serv.services;

import com.serv.database.entities.Photo;
import com.serv.database.entities.Video;
import com.serv.database.entities.Worker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MediaStorageServiceTest {

    @InjectMocks
    private MediaStorageService mediaStorageService;

    @TempDir
    Path tempDir;

    private Worker worker;

    @BeforeEach
    void setUp() {
        // Injection des propriétés @Value nécessaires au fonctionnement du service
        ReflectionTestUtils.setField(mediaStorageService, "uploadBase", tempDir.toString());
        ReflectionTestUtils.setField(mediaStorageService, "publicBaseUrl", "http://localhost/uploads");

        worker = new Worker();
        worker.setId(UUID.randomUUID());
        worker.setUsername("TestWorker");
    }

    @Test
    @DisplayName("savePhoto - Sauvegarde l'image originale et génère la miniature principale avec succès")
    void savePhoto_Success() throws IOException {
        // Création d'un faux fichier image valide (sous forme de tableau d'octets minimal ou factice)
        // Note : Thumbnailator nécessite un contenu image réel (ex: un petit PNG/JPG encodé)
        // ou un Mock si l'on mocke la partie Thumbnailator, mais ici on teste l'intégration disque.
        // Pour un test unitaire simple sans vraie image complexe, on peut fournir un flux d'octets.
        byte[] dummyImageBytes = createDummyImageBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                dummyImageBytes
        );

        Photo photo = mediaStorageService.savePhoto(file, worker);

        assertThat(photo).isNotNull();
        assertThat(photo.getUrl()).contains("/originals/");
        assertThat(photo.getMainThumbUrl()).contains("/thumbs/main/");
        assertThat(photo.getFileSize()).isEqualTo(file.getSize());
        assertThat(photo.getWorker()).isEqualTo(worker);
    }

    @Test
    @DisplayName("savePhoto - Échec si le fichier n'est pas une image")
    void savePhoto_InvalidContentType() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "Hello World".getBytes()
        );

        assertThatThrownBy(() -> mediaStorageService.savePhoto(textFile, worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only image files are accepted.");
    }

    @Test
    @DisplayName("saveVideo - Sauvegarde la vidéo avec succès")
    void saveVideo_Success() throws IOException {
        MockMultipartFile videoFile = new MockMultipartFile(
                "file",
                "clip.mp4",
                "video/mp4",
                new byte[1024]
        );

        Video video = mediaStorageService.saveVideo(videoFile, worker);

        assertThat(video).isNotNull();
        assertThat(video.getUrl()).contains("/videos/");
        assertThat(video.getFileSize()).isEqualTo(videoFile.getSize());
        assertThat(video.getWorker()).isEqualTo(worker);
    }

    @Test
    @DisplayName("saveVideo - Échec si le fichier n'est pas une vidéo")
    void saveVideo_InvalidContentType() {
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "pic.jpg",
                "image/jpeg",
                new byte[1024]
        );

        assertThatThrownBy(() -> mediaStorageService.saveVideo(imageFile, worker))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only video files are accepted.");
    }

    @Test
    @DisplayName("deleteAllForUser - Supprime tout le dossier physique de l'utilisateur")
    void deleteAllForUser_Success() throws IOException {
        // Sauvegarde d'abord un fichier pour créer l'arborescence
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", createDummyImageBytes());
        mediaStorageService.savePhoto(file, worker);

        Path userDir = tempDir.resolve(String.valueOf(worker.getId()));
        assertThat(userDir).exists();

        // Suppression totale
        mediaStorageService.deleteAllForUser(worker.getId());

        assertThat(userDir).doesNotExist();
    }

    // Utilitaire pour simuler des octets valides pour Thumbnailator
    private byte[] createDummyImageBytes() {
        // Utilisation d'un tout petit tableau ou d'un carré encodé si nécessaire,
        // ou test avec Thumbnailator. (Si Thumbnailator échoue sur un tableau vide,
        // on peut utiliser une image de test minimale en base64 ou un buffer).
        return new byte[512];
    }
}