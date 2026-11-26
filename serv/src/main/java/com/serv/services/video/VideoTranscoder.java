package com.serv.services.video;

import java.io.IOException;
import java.nio.file.Path;

/** Video probing and re-encoding (ffmpeg in production; replaced by a fake in most tests). */
public interface VideoTranscoder {

    /** What the file really contains, whatever its name or declared type. */
    record Probe(boolean hasVideo, double durationSeconds, int width, int height) {}

    Probe probe(Path input) throws IOException;

    /**
     * Re-encodes to a web-friendly MP4 (H.264 + AAC, longest side at most {@code maxSide}, start of file
     * ready for streaming) and drops every metadata (phones record the GPS position).
     */
    void transcode(Path input, Path output, int maxSide) throws IOException;

    /** One JPEG frame taken at {@code atSeconds}, at most {@code width} pixels wide. */
    void thumbnail(Path video, Path output, double atSeconds, int width) throws IOException;
}
