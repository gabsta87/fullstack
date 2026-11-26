package com.serv.services.video;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Runs the ffmpeg / ffprobe binaries (installed in the backend image). Untrusted input, so: only local files
 * may be opened ({@code -protocol_whitelist file}), no stdin, a time limit per command, bounded output.
 */
@Slf4j
@Component
public class FfmpegVideoTranscoder implements VideoTranscoder {

    private static final long PROBE_TIMEOUT_S = 30;
    private static final long THUMBNAIL_TIMEOUT_S = 60;
    private static final int MAX_CAPTURED_OUTPUT = 64 * 1024;

    @Value("${media.video.ffmpeg:ffmpeg}")
    private String ffmpeg;

    @Value("${media.video.ffprobe:ffprobe}")
    private String ffprobe;

    /** Re-encoding a long video on the NAS is slow, but never endless. */
    @Value("${media.video.transcode-timeout-seconds:900}")
    private long transcodeTimeoutSeconds;

    @Value("${media.video.threads:2}")
    private int threads;

    @Override
    public Probe probe(Path input) throws IOException {
        String out = run(List.of(ffprobe, "-v", "error", "-protocol_whitelist", "file",
                "-show_entries", "format=duration:stream=codec_type,width,height",
                "-of", "default=noprint_wrappers=1", input.toString()), PROBE_TIMEOUT_S);
        boolean hasVideo = false;
        double duration = 0;
        int width = 0, height = 0;
        for (String line : out.split("\\R")) {
            String[] kv = line.split("=", 2);
            if (kv.length != 2) continue;
            switch (kv[0]) {
                case "codec_type" -> hasVideo |= kv[1].equals("video");
                case "duration" -> duration = Math.max(duration, parseDouble(kv[1]));
                case "width" -> width = Math.max(width, (int) parseDouble(kv[1]));
                case "height" -> height = Math.max(height, (int) parseDouble(kv[1]));
                default -> { }
            }
        }
        return new Probe(hasVideo, duration, width, height);
    }

    @Override
    public void transcode(Path input, Path output, int maxSide) throws IOException {
        // Longest side at most maxSide, never upscaled, even dimensions (required by H.264)
        String scale = String.format(Locale.ROOT,
                "scale='if(gt(iw,ih),min(%1$d,iw),-2)':'if(gt(iw,ih),-2,min(%1$d,ih))'", maxSide);
        run(List.of(ffmpeg, "-nostdin", "-hide_banner", "-loglevel", "error", "-protocol_whitelist", "file",
                "-i", input.toString(),
                "-map", "0:v:0", "-map", "0:a:0?",
                "-map_metadata", "-1", "-map_metadata:s:v", "-1", "-map_metadata:s:a", "-1", "-map_chapters", "-1",
                "-vf", scale, "-fpsmax", "30",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "28", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-b:a", "96k", "-ac", "2",
                "-movflags", "+faststart", "-threads", String.valueOf(threads),
                "-f", "mp4", "-y", output.toString()), transcodeTimeoutSeconds);
    }

    @Override
    public void thumbnail(Path video, Path output, double atSeconds, int width) throws IOException {
        run(List.of(ffmpeg, "-nostdin", "-hide_banner", "-loglevel", "error", "-protocol_whitelist", "file",
                "-ss", String.format(Locale.ROOT, "%.2f", atSeconds), "-i", video.toString(),
                "-frames:v", "1", "-vf", String.format(Locale.ROOT, "scale='min(%d,iw)':-2", width),
                "-map_metadata", "-1", "-q:v", "3", "-f", "image2", "-y", output.toString()), THUMBNAIL_TIMEOUT_S);
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    /** Runs a command; its output (stdout + stderr, truncated) is returned, or the reason of the failure thrown. */
    private static String run(List<String> command, long timeoutSeconds) throws IOException {
        Process process = new ProcessBuilder(new ArrayList<>(command)).redirectErrorStream(true).start();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        Thread drain = Thread.ofVirtual().start(() -> {
            try (InputStream in = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    synchronized (captured) {
                        if (captured.size() < MAX_CAPTURED_OUTPUT) captured.write(buffer, 0, n);
                    }
                }
            } catch (IOException ignored) {
                // process ended
            }
        });
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException(command.getFirst() + " timed out after " + timeoutSeconds + " s");
            }
            drain.join(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException(command.getFirst() + " interrupted", e);
        }
        String output;
        synchronized (captured) {
            output = captured.toString(StandardCharsets.UTF_8);
        }
        if (process.exitValue() != 0) {
            throw new IOException(command.getFirst() + " failed (" + process.exitValue() + "): "
                    + output.lines().limit(3).reduce("", (a, b) -> a + " " + b).trim());
        }
        return output;
    }
}
