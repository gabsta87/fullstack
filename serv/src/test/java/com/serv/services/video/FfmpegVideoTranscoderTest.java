package com.serv.services.video;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Runs the real ffmpeg (installed in the CI and in the backend image); skipped where it is missing. */
class FfmpegVideoTranscoderTest {

    @TempDir static Path dir;
    static Path source;
    final FfmpegVideoTranscoder transcoder = new FfmpegVideoTranscoder();

    {
        ReflectionTestUtils.setField(transcoder, "ffmpeg", "ffmpeg");
        ReflectionTestUtils.setField(transcoder, "ffprobe", "ffprobe");
        ReflectionTestUtils.setField(transcoder, "transcodeTimeoutSeconds", 120L);
        ReflectionTestUtils.setField(transcoder, "threads", 2);
    }

    private static String run(String... command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor(60, TimeUnit.SECONDS);
        return out;
    }

    @BeforeAll
    static void sourceVideo() throws Exception {
        boolean available;
        try {
            available = run("ffmpeg", "-version").startsWith("ffmpeg");
        } catch (IOException e) {
            available = false;
        }
        assumeTrue(available, "ffmpeg not installed");
        // 3 s of 1080p at 60 fps with sound and a GPS position, like a phone video
        source = dir.resolve("phone.mov");
        run("ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", "testsrc=size=1920x1080:rate=60:duration=3",
                "-f", "lavfi", "-i", "sine=frequency=440:duration=3",
                "-metadata", "location=+46.2044+006.1432/", "-metadata", "title=secret",
                "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "aac", "-shortest", source.toString());
        assumeTrue(Files.exists(source), "test video could not be generated");
    }

    @Test
    @DisplayName("Probe reads the real content")
    void probe() throws IOException {
        VideoTranscoder.Probe probe = transcoder.probe(source);
        assertThat(probe.hasVideo()).isTrue();
        assertThat(probe.durationSeconds()).isBetween(2.9, 3.2);
        assertThat(probe.width()).isEqualTo(1920);
    }

    @Test
    @DisplayName("Re-encoded to 720p at 30 fps at most, without metadata; thumbnail is a JPEG")
    void transcodeAndThumbnail() throws Exception {
        Path out = dir.resolve("out.mp4");
        transcoder.transcode(source, out, 1280);
        VideoTranscoder.Probe probe = transcoder.probe(out);
        assertThat(probe.width()).isEqualTo(1280);
        assertThat(probe.height()).isEqualTo(720);

        String tags = run("ffprobe", "-v", "error", "-show_entries", "format_tags:stream_tags", "-of", "default", out.toString());
        assertThat(tags).doesNotContain("location").doesNotContain("secret");
        String fps = run("ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=avg_frame_rate",
                "-of", "default=noprint_wrappers=1:nokey=1", out.toString()).trim();
        assertThat(fps).isEqualTo("30/1");

        Path thumb = dir.resolve("thumb.jpg");
        transcoder.thumbnail(out, thumb, 1.0, 600);
        byte[] jpeg = Files.readAllBytes(thumb);
        assertThat(jpeg[0] & 0xFF).isEqualTo(0xFF);
        assertThat(jpeg[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    @DisplayName("A file that is not a video is reported as such, and other protocols cannot be opened")
    void notAVideo() throws IOException {
        Path html = dir.resolve("fake.mp4");
        Files.writeString(html, "<html><script>alert(1)</script></html>");
        assertThatThrownBy(() -> transcoder.probe(html)).isInstanceOf(IOException.class);

        // A playlist pointing to a network address must not be followed (SSRF)
        Path playlist = dir.resolve("list.m3u8");
        Files.writeString(playlist, "#EXTM3U\n#EXTINF:1,\nhttp://169.254.169.254/latest/meta-data\n");
        VideoTranscoder.Probe probe;
        try {
            probe = transcoder.probe(playlist);
        } catch (IOException refused) {
            return; // refused outright: fine
        }
        assertThat(probe.hasVideo()).isFalse();
    }
}
