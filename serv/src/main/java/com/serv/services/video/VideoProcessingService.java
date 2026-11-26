package com.serv.services.video;

import com.serv.common.VideoStatus;
import com.serv.database.entities.Video;
import com.serv.database.entities.Worker;
import com.serv.database.repositories.VideoRepository;
import com.serv.database.repositories.WorkerRepository;
import com.serv.dto.WorkerFullProfileDTO;
import com.serv.services.MediaStorageService;
import com.serv.services.SseStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Turns an uploaded video into what visitors see, in the background (one at a time: the NAS CPU is small):
 * checks it really is a video of acceptable length, re-encodes it (smaller, no metadata), extracts a
 * thumbnail, then marks it READY and updates the worker's page live. Anything wrong marks it FAILED with a
 * reason the worker can read. Raw uploads never reach the public directory.
 */
@Slf4j
@Service
public class VideoProcessingService {

    /** Longest accepted video. */
    public static final int MAX_DURATION_SECONDS = 120;
    /** Longest side of the stored video (720p for a landscape 16:9 video). */
    static final int MAX_SIDE = 1280;
    static final int THUMBNAIL_WIDTH = 600;

    public static final String REASON_UNREADABLE = "VIDEO_UNREADABLE";
    public static final String REASON_TOO_LONG = "VIDEO_TOO_LONG";
    public static final String REASON_FAILED = "VIDEO_PROCESSING_FAILED";

    private final VideoRepository videoRepository;
    private final WorkerRepository workerRepository;
    private final MediaStorageService storage;
    private final VideoTranscoder transcoder;
    private final SseStreamService sseStreamService;
    private final TransactionTemplate tx;
    private final Executor executor;

    public VideoProcessingService(VideoRepository videoRepository, WorkerRepository workerRepository,
                                  MediaStorageService storage, VideoTranscoder transcoder,
                                  SseStreamService sseStreamService, TransactionTemplate tx,
                                  @Qualifier("videoProcessingExecutor") Executor executor) {
        this.videoRepository = videoRepository;
        this.workerRepository = workerRepository;
        this.storage = storage;
        this.transcoder = transcoder;
        this.sseStreamService = sseStreamService;
        this.tx = tx;
        this.executor = executor;
    }

    /** Queues the video once the current transaction (that saved it) has committed. */
    public void enqueue(UUID videoId) {
        Runnable submit = () -> {
            try {
                executor.execute(() -> process(videoId));
            } catch (RejectedExecutionException e) {
                log.warn("Video {} not queued (queue full): marked failed", videoId);
                finish(videoId, VideoStatus.FAILED, REASON_FAILED, null, 0L);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { submit.run(); }
            });
        } else {
            submit.run();
        }
    }

    /** Videos left half-processed by a restart are processed again. */
    @EventListener(ApplicationReadyEvent.class)
    public void resumePending() {
        var pending = videoRepository.findIdsByStatus(VideoStatus.PROCESSING);
        if (!pending.isEmpty()) log.info("Resuming the processing of {} video(s)", pending.size());
        pending.forEach(this::enqueue);
    }

    void process(UUID videoId) {
        Video video = tx.execute(s -> videoRepository.findById(videoId).orElse(null));
        if (video == null || video.getStatus() != VideoStatus.PROCESSING) return;
        UUID workerId = tx.execute(s -> videoRepository.findWorkerIdById(videoId));
        if (workerId == null) return;

        Path pending = null, encoding = null, output = null, thumbnail = null;
        try {
            pending = storage.pendingVideoFile(workerId, video);
            encoding = storage.encodingVideoFile(workerId, video);
            output = storage.publicVideoFile(workerId, video);
            thumbnail = storage.publicVideoThumbnail(workerId, video);
            if (!Files.exists(pending)) {
                finish(videoId, VideoStatus.FAILED, REASON_FAILED, null, 0L);
                return;
            }

            VideoTranscoder.Probe probe = transcoder.probe(pending);
            if (!probe.hasVideo() || probe.durationSeconds() <= 0) {
                log.warn("SECURITY video {} of worker {} refused: not a readable video", videoId, workerId);
                finish(videoId, VideoStatus.FAILED, REASON_UNREADABLE, null, 0L);
                return;
            }
            if (probe.durationSeconds() > MAX_DURATION_SECONDS) {
                finish(videoId, VideoStatus.FAILED, REASON_TOO_LONG, (int) Math.round(probe.durationSeconds()), 0L);
                return;
            }

            transcoder.transcode(pending, encoding, MAX_SIDE);
            transcoder.thumbnail(encoding, thumbnail, Math.min(1.0, probe.durationSeconds() / 2), THUMBNAIL_WIDTH);
            moveInPlace(encoding, output);

            boolean kept = finish(videoId, VideoStatus.READY, null, (int) Math.round(probe.durationSeconds()), Files.size(output));
            if (!kept) { // deleted by the worker meanwhile
                Files.deleteIfExists(output);
                Files.deleteIfExists(thumbnail);
            }
            log.info("Video {} of worker {} ready ({} s)", videoId, workerId, Math.round(probe.durationSeconds()));
        } catch (Exception e) {
            log.warn("Video {} of worker {} could not be processed: {}", videoId, workerId, e.getMessage());
            finish(videoId, VideoStatus.FAILED, REASON_FAILED, null, 0L);
            deleteQuietly(output);
            deleteQuietly(thumbnail);
        } finally {
            deleteQuietly(pending);
            deleteQuietly(encoding);
        }
    }

    /** Records the outcome and refreshes the worker's page; false if the video no longer exists. */
    private boolean finish(UUID videoId, VideoStatus status, String reason, Integer duration, long size) {
        Boolean kept = tx.execute(s -> videoRepository.findById(videoId).map(v -> {
            v.setStatus(status);
            v.setFailureReason(reason);
            if (duration != null) v.setDuration(duration);
            v.setFileSize(size);
            videoRepository.save(v);
            Worker worker = workerRepository.findById(v.getWorker().getId()).orElse(null);
            if (worker != null) {
                sseStreamService.emitEvent(worker.getId(), "account-update", WorkerFullProfileDTO.from(worker));
            }
            return true;
        }).orElse(false));
        return Boolean.TRUE.equals(kept);
    }

    /**
     * The private and public folders may be different volumes (Docker): then the file is copied. Not atomic,
     * but visitors only get the URL once the video is READY, after this.
     */
    private static void moveInPlace(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // nothing more to do
        }
    }
}
