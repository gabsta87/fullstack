package com.serv.features;

import com.serv.common.VideoStatus;
import com.serv.database.entities.Video;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.VideoRepository;
import com.serv.services.video.VideoTranscoder;
import com.serv.support.IntegrationTest;
import com.serv.support.TestImages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VideoProcessingTest extends IntegrationTest {

    @Autowired VideoRepository videoRepository;

    private Worker worker;

    private Video uploadAndWait(VideoStatus expected) throws Exception {
        worker = createVisibleWorker("luna@venus.test");
        mvc.perform(multipart("/account/worker/media")
                        .file(new MockMultipartFile("files", "clip.mp4", "video/mp4", TestImages.mp4()))
                        .header("Authorization", bearer(worker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videos[0].status").value("PROCESSING"));
        for (int i = 0; i < 100; i++) {
            List<Video> videos = videoRepository.findByWorkerId(worker.getId());
            if (!videos.isEmpty() && videos.getFirst().getStatus() == expected) return videos.getFirst();
            Thread.sleep(50);
        }
        throw new AssertionError("Video never reached " + expected);
    }

    private void fakeTranscoder(VideoTranscoder.Probe probe) throws IOException {
        when(videoTranscoder.probe(any())).thenReturn(probe);
        doAnswer(inv -> Files.write(inv.getArgument(1), new byte[300])).when(videoTranscoder).transcode(any(), any(), anyInt());
        doAnswer(inv -> Files.write(inv.getArgument(1), new byte[30])).when(videoTranscoder).thumbnail(any(), any(), anyDouble(), anyInt());
    }

    private List<Path> files(Path root) throws IOException {
        Path dir = root.resolve(worker.getId().toString());
        if (!Files.exists(dir)) return List.of();
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile).toList();
        }
    }

    @Test
    @DisplayName("A video is re-encoded with a thumbnail, then shown to visitors; the raw upload is deleted")
    void ready() throws Exception {
        fakeTranscoder(new VideoTranscoder.Probe(true, 12.4, 1920, 1080));
        Video video = uploadAndWait(VideoStatus.READY);

        assertThat(video.getDuration()).isEqualTo(12);
        assertThat(video.getFileSize()).isEqualTo(300);
        assertThat(files(MEDIA_DIR)).extracting(p -> MEDIA_DIR.relativize(p).toString())
                .containsExactlyInAnyOrder(
                        worker.getId() + "/videos/" + fileName(video.getUrl()),
                        worker.getId() + "/thumbs/videos/" + fileName(video.getMainThumbUrl()));
        assertThat(files(PRIVATE_MEDIA_DIR)).isEmpty();

        mvc.perform(get("/workers/" + worker.getId()))
                .andExpect(jsonPath("$.videos.length()").value(1))
                .andExpect(jsonPath("$.videos[0].thumbnailUrl").value(video.getMainThumbUrl()));
    }

    private static String fileName(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    @Test
    @DisplayName("Too long, unreadable or failing videos are refused with a reason, never published")
    void failures() throws Exception {
        fakeTranscoder(new VideoTranscoder.Probe(true, 300, 1920, 1080));
        Video tooLong = uploadAndWait(VideoStatus.FAILED);
        assertThat(tooLong.getFailureReason()).isEqualTo("VIDEO_TOO_LONG");
        assertThat(files(MEDIA_DIR)).isEmpty();
        assertThat(files(PRIVATE_MEDIA_DIR)).isEmpty();
        mvc.perform(get("/workers/" + worker.getId())).andExpect(jsonPath("$.videos.length()").value(0));
        mvc.perform(get("/account/worker/me").header("Authorization", bearer(worker)))
                .andExpect(jsonPath("$.videos[0].status").value("FAILED"))
                .andExpect(jsonPath("$.videos[0].failureReason").value("VIDEO_TOO_LONG"));
    }

    @Test
    @DisplayName("A file with no video stream is refused")
    void unreadable() throws Exception {
        fakeTranscoder(new VideoTranscoder.Probe(false, 0, 0, 0));
        assertThat(uploadAndWait(VideoStatus.FAILED).getFailureReason()).isEqualTo("VIDEO_UNREADABLE");
        assertThat(files(MEDIA_DIR)).isEmpty();
    }

    @Test
    @DisplayName("An encoding error leaves no file behind")
    void encodingError() throws Exception {
        fakeTranscoder(new VideoTranscoder.Probe(true, 5, 640, 480));
        doThrow(new IOException("ffmpeg failed")).when(videoTranscoder).transcode(any(), any(), anyInt());
        assertThat(uploadAndWait(VideoStatus.FAILED).getFailureReason()).isEqualTo("VIDEO_PROCESSING_FAILED");
        assertThat(files(MEDIA_DIR)).isEmpty();
        assertThat(files(PRIVATE_MEDIA_DIR)).isEmpty();
    }
}
