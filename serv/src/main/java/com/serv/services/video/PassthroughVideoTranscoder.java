package com.serv.services.video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Local development without ffmpeg ({@code media.video.engine=passthrough}, never in production):
 * the video is published as sent, with a plain thumbnail. Nothing is checked or re-encoded, and the
 * metadata (GPS position) is kept, so the full upload cycle can be tried, not the processing itself.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "media.video.engine", havingValue = "passthrough")
public class PassthroughVideoTranscoder implements VideoTranscoder {

    public PassthroughVideoTranscoder(org.springframework.core.env.Environment environment) {
        // Unprocessed videos would keep the GPS position of the phone: refused outright in production
        if (environment.matchesProfiles("prod")) {
            throw new IllegalStateException("media.video.engine=passthrough is for development only");
        }
        log.info("Videos are published without processing (media.video.engine=passthrough, development only)");
    }

    /** The real content is unknown without ffmpeg: a short video is assumed, so it is accepted. */
    @Override
    public Probe probe(Path input) {
        return new Probe(true, 1.0, 0, 0);
    }

    @Override
    public void transcode(Path input, Path output, int maxSide) throws IOException {
        Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
    }

    /** A dark frame with a "play" triangle, at the requested width (16:9). */
    @Override
    public void thumbnail(Path video, Path output, double atSeconds, int width) throws IOException {
        int height = Math.max(1, width * 9 / 16);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(40, 36, 33));
            g.fillRect(0, 0, width, height);
            g.setColor(new Color(235, 230, 225));
            int s = height / 4;
            g.fillPolygon(new int[]{width / 2 - s / 2, width / 2 - s / 2, width / 2 + s / 2},
                          new int[]{height / 2 - s / 2, height / 2 + s / 2, height / 2}, 3);
        } finally {
            g.dispose();
        }
        if (!ImageIO.write(image, "jpg", output.toFile())) throw new IOException("No JPEG writer");
    }
}
