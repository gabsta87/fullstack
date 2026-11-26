package com.serv.services.video;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PassthroughVideoTranscoderTest {

    @TempDir Path dir;

    @Test
    @DisplayName("Development without ffmpeg: the video is kept as sent and gets a JPEG thumbnail")
    void keepsTheVideo() throws Exception {
        PassthroughVideoTranscoder t = new PassthroughVideoTranscoder(new MockEnvironment().withProperty("x", "y"));
        Path in = Files.write(dir.resolve("in.mov"), new byte[]{1, 2, 3, 4});
        Path out = dir.resolve("out.mp4");

        assertThat(t.probe(in).hasVideo()).isTrue();
        t.transcode(in, out, 1280);
        assertThat(Files.readAllBytes(out)).containsExactly(1, 2, 3, 4);

        Path thumb = dir.resolve("t.jpg");
        t.thumbnail(out, thumb, 0.5, 600);
        byte[] jpeg = Files.readAllBytes(thumb);
        assertThat(jpeg[0] & 0xFF).isEqualTo(0xFF);
        assertThat(jpeg[1] & 0xFF).isEqualTo(0xD8);
    }

    @Test
    @DisplayName("Refused in production: unprocessed videos would keep the GPS position")
    void neverInProduction() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        assertThatThrownBy(() -> new PassthroughVideoTranscoder(prod)).isInstanceOf(IllegalStateException.class);
    }
}
