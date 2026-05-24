package io.github.yienruuuuu.xtracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskStatus;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import io.github.yienruuuuu.scheduler.service.ScheduledTaskService;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerBackfillJobRequest;
import io.github.yienruuuuu.xtracker.bean.dto.XTrackerSyncOptions;
import io.github.yienruuuuu.xtracker.bean.po.XTrackerBackfillJobEntity;
import io.github.yienruuuuu.xtracker.bean.po.XTrackerBackfillWindowEntity;
import io.github.yienruuuuu.xtracker.dao.XTrackerBackfillJobDao;
import io.github.yienruuuuu.xtracker.dao.XTrackerBackfillWindowDao;
import io.github.yienruuuuu.xtracker.domain.XTrackerBackfillStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class XTrackerBackfillJobService {

    private static final String DEFAULT_PLATFORM = "X";
    private static final String DEFAULT_HANDLE = "elonmusk";
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final XTrackerBackfillJobDao jobDao;
    private final XTrackerBackfillWindowDao windowDao;
    private final XTrackerPersonSyncService syncService;
    private final XTrackerBackfillDiscoveryService discoveryService;
    private final ScheduledTaskService scheduledTaskService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final Duration lockTtl;
    private final String lockOwner;

    public XTrackerBackfillJobService(
            XTrackerBackfillJobDao jobDao,
            XTrackerBackfillWindowDao windowDao,
            XTrackerPersonSyncService syncService,
            XTrackerBackfillDiscoveryService discoveryService,
            ScheduledTaskService scheduledTaskService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            @Value("${xtracker.backfill.batch-size:3}") int batchSize,
            @Value("${xtracker.backfill.lock-ttl:PT5M}") Duration lockTtl
    ) {
        this.jobDao = jobDao;
        this.windowDao = windowDao;
        this.syncService = syncService;
        this.discoveryService = discoveryService;
        this.scheduledTaskService = scheduledTaskService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.batchSize = batchSize;
        this.lockTtl = lockTtl;
        this.lockOwner = resolveLockOwner();
    }

    @Transactional
    public XTrackerBackfillJobEntity createJob(XTrackerBackfillJobRequest request) {
        String platform = normalizePlatform(request == null ? null : request.platform());
        String handle = normalizeHandle(request == null ? null : request.handle());
        Instant cutoffAt = Instant.now();
        Instant earliestAt = request == null || request.earliestAt() == null
                ? discoveryService.discoverEarliestAt(platform, handle, cutoffAt)
                : request.earliestAt();
        if (!earliestAt.isBefore(cutoffAt)) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "earliestAt must be before current time");
        }
        boolean enableRealtimeAfter = request == null
                || request.enableRealtimeAfterBackfill() == null
                || request.enableRealtimeAfterBackfill();

        String realtimePayload = payload(platform, handle);
        UUID realtimeTaskId = scheduledTaskService.ensureCronTask(
                ScheduledTaskType.XTRACKER_PERSON_POST_REALTIME_SYNC,
                realtimePayload,
                DEFAULT_MAX_ATTEMPTS,
                ScheduledTaskStatus.DISABLED
        );
        scheduledTaskService.ensureCronTask(
                ScheduledTaskType.XTRACKER_PERSON_POST_BACKFILL_WORKER,
                "{}",
                DEFAULT_MAX_ATTEMPTS,
                ScheduledTaskStatus.ACTIVE
        );

        XTrackerBackfillJobEntity job = new XTrackerBackfillJobEntity();
        job.setPlatform(platform);
        job.setHandle(handle);
        job.setEarliestAt(earliestAt);
        job.setCutoffAt(cutoffAt);
        job.setEnableRealtimeAfter(enableRealtimeAfter);
        job.setRealtimeTaskId(realtimeTaskId);
        job.setStatus(XTrackerBackfillStatus.PENDING);
        job = jobDao.save(job);

        List<WindowRange> ranges = buildDailyUtcWindows(earliestAt, cutoffAt);
        for (WindowRange range : ranges) {
            XTrackerBackfillWindowEntity window = new XTrackerBackfillWindowEntity();
            window.setJob(job);
            window.setPlatform(platform);
            window.setHandle(handle);
            window.setStartAt(range.startAt());
            window.setEndAt(range.endAt());
            window.setStatus(XTrackerBackfillStatus.PENDING);
            window.setAttempt(0);
            window.setMaxAttempts(DEFAULT_MAX_ATTEMPTS);
            window.setNextRunAt(Instant.now());
            windowDao.save(window);
        }
        job.setTotalWindows(ranges.size());
        log.info("XTracker backfill job created. jobId={}, platform={}, handle={}, earliestAt={}, cutoffAt={}, windows={}",
                job.getId(), platform, handle, earliestAt, cutoffAt, ranges.size());
        return job;
    }

    public List<XTrackerBackfillWindowEntity> claimDueWindows() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            List<XTrackerBackfillWindowEntity> windows = windowDao.findDueWindowsForUpdate(now, batchSize);
            for (XTrackerBackfillWindowEntity window : windows) {
                window.setStatus(XTrackerBackfillStatus.RUNNING);
                window.setLockOwner(lockOwner);
                window.setLockUntil(now.plus(lockTtl));
                window.setLastStartedAt(now);
            }
            log.info("XTracker backfill windows claimed. count={}, batchSize={}, lockOwner={}, lockUntil={}",
                    windows.size(), batchSize, lockOwner, now.plus(lockTtl));
            return windows;
        });
    }

    public void processDueWindows() {
        List<XTrackerBackfillWindowEntity> windows = claimDueWindows();
        if (windows.isEmpty()) {
            log.info("XTracker backfill worker found no due windows.");
            return;
        }
        for (XTrackerBackfillWindowEntity window : windows) {
            processWindow(window.getId());
        }
    }

    @Transactional(readOnly = true)
    public boolean hasActiveBackfillWork() {
        return windowDao.countByStatusIn(List.of(XTrackerBackfillStatus.PENDING, XTrackerBackfillStatus.RUNNING)) > 0;
    }

    void processWindow(UUID windowId) {
        XTrackerBackfillWindowEntity window = windowDao.findById(windowId)
                .orElseThrow(() -> new IllegalStateException("Claimed XTracker backfill window disappeared: " + windowId));
        try {
            log.info("XTracker backfill window started. windowId={}, platform={}, handle={}, startAt={}, endAt={}",
                    window.getId(), window.getPlatform(), window.getHandle(), window.getStartAt(), window.getEndAt());
            syncService.syncOnce(
                    window.getPlatform(),
                    window.getHandle(),
                    new XTrackerSyncOptions(false, window.getStartAt(), window.getEndAt(), null)
            );
            markWindowCompleted(window.getId());
            log.info("XTracker backfill window completed. windowId={}, platform={}, handle={}, startAt={}, endAt={}",
                    window.getId(), window.getPlatform(), window.getHandle(), window.getStartAt(), window.getEndAt());
        } catch (Exception exception) {
            log.warn("XTracker backfill window failed. windowId={}, platform={}, handle={}, startAt={}, endAt={}",
                    window.getId(), window.getPlatform(), window.getHandle(), window.getStartAt(), window.getEndAt(), exception);
            markWindowFailed(window.getId(), exception);
        }
        refreshJob(window.getJob().getId());
    }

    public void markWindowCompleted(UUID windowId) {
        transactionTemplate.executeWithoutResult(status -> {
            XTrackerBackfillWindowEntity window = windowDao.findById(windowId)
                    .orElseThrow(() -> new IllegalStateException("XTracker backfill window not found: " + windowId));
            Instant now = Instant.now();
            window.setStatus(XTrackerBackfillStatus.COMPLETED);
            window.setLockOwner(null);
            window.setLockUntil(null);
            window.setLastFinishedAt(now);
            window.setCompletedAt(now);
            window.setErrorMessage(null);
            log.info("XTracker backfill window completion persisted. windowId={}, completedAt={}",
                    window.getId(), now);
        });
    }

    public void markWindowFailed(UUID windowId, Exception exception) {
        transactionTemplate.executeWithoutResult(status -> {
            XTrackerBackfillWindowEntity window = windowDao.findById(windowId)
                    .orElseThrow(() -> new IllegalStateException("XTracker backfill window not found: " + windowId));
            Instant now = Instant.now();
            int nextAttempt = window.getAttempt() + 1;
            window.setAttempt(nextAttempt);
            window.setLockOwner(null);
            window.setLockUntil(null);
            window.setLastFinishedAt(now);
            window.setErrorMessage(exception.getMessage());
            if (nextAttempt >= window.getMaxAttempts()) {
                window.setStatus(XTrackerBackfillStatus.FAILED);
                log.warn("XTracker backfill window marked failed. windowId={}, attempt={}, maxAttempts={}",
                        window.getId(), nextAttempt, window.getMaxAttempts());
                return;
            }
            Instant nextRunAt = now.plus(retryDelay(nextAttempt));
            window.setStatus(XTrackerBackfillStatus.PENDING);
            window.setNextRunAt(nextRunAt);
            log.warn("XTracker backfill window retry scheduled. windowId={}, attempt={}, nextRunAt={}",
                    window.getId(), nextAttempt, nextRunAt);
        });
    }

    public void refreshJob(UUID jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            XTrackerBackfillJobEntity job = jobDao.findById(jobId)
                    .orElseThrow(() -> new IllegalStateException("XTracker backfill job not found: " + jobId));
            int total = (int) windowDao.countByJobId(jobId);
            int completed = (int) windowDao.countByJobIdAndStatus(jobId, XTrackerBackfillStatus.COMPLETED);
            int failed = (int) windowDao.countByJobIdAndStatus(jobId, XTrackerBackfillStatus.FAILED);
            job.setTotalWindows(total);
            job.setCompletedWindows(completed);
            job.setFailedWindows(failed);
            if (failed > 0) {
                job.setStatus(XTrackerBackfillStatus.FAILED);
                job.setErrorMessage("One or more backfill windows failed");
                log.warn("XTracker backfill job marked failed. jobId={}, total={}, completed={}, failed={}",
                        job.getId(), total, completed, failed);
                return;
            }
            if (total > 0 && completed == total) {
                job.setStatus(XTrackerBackfillStatus.COMPLETED);
                job.setCompletedAt(Instant.now());
                if (job.isEnableRealtimeAfter() && job.getRealtimeTaskId() != null) {
                    scheduledTaskService.enable(job.getRealtimeTaskId());
                    log.info("XTracker realtime task enabled after backfill. jobId={}, taskId={}",
                            job.getId(), job.getRealtimeTaskId());
                }
                log.info("XTracker backfill job completed. jobId={}, total={}, completed={}, failed={}",
                        job.getId(), total, completed, failed);
                return;
            }
            job.setStatus(completed > 0 ? XTrackerBackfillStatus.RUNNING : XTrackerBackfillStatus.PENDING);
            log.info("XTracker backfill job progress refreshed. jobId={}, status={}, total={}, completed={}, failed={}",
                    job.getId(), job.getStatus(), total, completed, failed);
        });
    }

    List<WindowRange> buildDailyUtcWindows(Instant earliestAt, Instant cutoffAt) {
        List<WindowRange> windows = new ArrayList<>();
        Instant cursor = earliestAt;
        while (cursor.isBefore(cutoffAt)) {
            LocalDate cursorDate = LocalDate.ofInstant(cursor, ZoneOffset.UTC);
            Instant nextMidnight = cursorDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant endAt = nextMidnight.isBefore(cutoffAt) ? nextMidnight : cutoffAt;
            windows.add(new WindowRange(cursor, endAt));
            cursor = endAt;
        }
        return windows;
    }

    private Duration retryDelay(int attempt) {
        return switch (attempt) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            default -> Duration.ofMinutes(15);
        };
    }

    private String payload(String platform, String handle) {
        try {
            return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("platform", platform)
                    .put("handle", handle));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize XTracker scheduler payload", exception);
        }
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return DEFAULT_PLATFORM;
        }
        return platform.trim().toUpperCase();
    }

    private String normalizeHandle(String handle) {
        if (handle == null || handle.isBlank()) {
            return DEFAULT_HANDLE;
        }
        return handle.trim().replaceFirst("^@", "");
    }

    private String resolveLockOwner() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException ex) {
            return "unknown-" + UUID.randomUUID();
        }
    }

    record WindowRange(Instant startAt, Instant endAt) {
    }
}
