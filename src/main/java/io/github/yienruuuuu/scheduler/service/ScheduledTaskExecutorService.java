package io.github.yienruuuuu.scheduler.service;

import io.github.yienruuuuu.scheduler.dao.ScheduledTaskDao;
import io.github.yienruuuuu.scheduler.dao.ScheduledTaskErrorDao;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskContext;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskHandler;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskStatus;
import io.github.yienruuuuu.scheduler.bean.po.ScheduledTaskEntity;
import io.github.yienruuuuu.scheduler.bean.po.ScheduledTaskErrorEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Background executor that claims due tasks, invokes handlers, applies retry
 * rules, and persists execution failures.
 */
@Slf4j
@Service
public class ScheduledTaskExecutorService {

    private static final ZoneId SCHEDULER_ZONE = ZoneId.of("Asia/Taipei");
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(15)
    );

    private final ScheduledTaskDao scheduledTaskDao;
    private final ScheduledTaskErrorDao scheduledTaskErrorDao;
    private final ScheduledTaskRegistry scheduledTaskRegistry;
    private final int batchSize;
    private final Duration lockTtl;
    private final String lockOwner;

    public ScheduledTaskExecutorService(
            ScheduledTaskDao scheduledTaskDao,
            ScheduledTaskErrorDao scheduledTaskErrorDao,
            ScheduledTaskRegistry scheduledTaskRegistry,
            @Value("${scheduler.task.batch-size:10}") int batchSize,
            @Value("${scheduler.task.lock-ttl:PT5M}") Duration lockTtl
    ) {
        this.scheduledTaskDao = scheduledTaskDao;
        this.scheduledTaskErrorDao = scheduledTaskErrorDao;
        this.scheduledTaskRegistry = scheduledTaskRegistry;
        this.batchSize = batchSize;
        this.lockTtl = lockTtl;
        this.lockOwner = resolveLockOwner();
    }

    @Scheduled(fixedDelayString = "${scheduler.task.scan-delay:PT10S}")
    public void executeDueTasks() {
        List<ScheduledTaskEntity> tasks = claimDueTasks();
        for (ScheduledTaskEntity task : tasks) {
            executeTask(task);
        }
    }

    @Transactional
    public List<ScheduledTaskEntity> claimDueTasks() {
        Instant now = Instant.now();
        List<ScheduledTaskEntity> tasks = scheduledTaskDao.findDueTasksForUpdate(now, batchSize);
        for (ScheduledTaskEntity task : tasks) {
            // Keep lock state in the same transaction as the SELECT FOR UPDATE.
            task.setStatus(ScheduledTaskStatus.RUNNING);
            task.setLockOwner(lockOwner);
            task.setLockUntil(now.plus(lockTtl));
            task.setLastStartedAt(now);
        }
        return tasks;
    }

    void executeTask(ScheduledTaskEntity task) {
        Optional<ScheduledTaskHandler> handler = scheduledTaskRegistry.find(task.getTaskType());
        if (handler.isEmpty()) {
            IllegalStateException error = new IllegalStateException("No handler for task type: " + task.getTaskType());
            markFailed(task.getId(), error, false);
            return;
        }

        try {
            ScheduledTaskContext context = new ScheduledTaskContext(
                    task.getId(),
                    handler.get().taskType(),
                    task.getPayload(),
                    task.getAttempt() + 1
            );
            handler.get().handle(context);
            markSucceeded(task.getId());
        } catch (Exception ex) {
            log.warn("Scheduled task failed. taskId={}, taskType={}", task.getId(), task.getTaskType(), ex);
            markFailed(task.getId(), ex, true);
        }
    }

    @Transactional
    public void markSucceeded(UUID taskId) {
        ScheduledTaskEntity task = scheduledTaskDao.findTaskById(taskId)
                .orElseThrow(() -> new IllegalStateException("Claimed task disappeared: " + taskId));
        Instant now = Instant.now();
        task.setStatus(ScheduledTaskStatus.ACTIVE);
        task.setAttempt(0);
        task.setNextRunAt(nextRunAt(task.getCronExpression(), now));
        task.setLockOwner(null);
        task.setLockUntil(null);
        task.setLastFinishedAt(now);
    }

    @Transactional
    public void markFailed(UUID taskId, Exception exception, boolean retryable) {
        ScheduledTaskEntity task = scheduledTaskDao.findTaskById(taskId)
                .orElseThrow(() -> new IllegalStateException("Claimed task disappeared: " + taskId));
        int nextAttempt = task.getAttempt() + 1;
        Instant now = Instant.now();

        ScheduledTaskErrorEntity error = new ScheduledTaskErrorEntity();
        error.setTask(task);
        error.setAttempt(nextAttempt);
        error.setErrorType(exception.getClass().getName());
        error.setErrorMessage(exception.getMessage());
        error.setStackTrace(stackTraceOf(exception));
        scheduledTaskErrorDao.save(error);

        task.setAttempt(nextAttempt);
        task.setLockOwner(null);
        task.setLockUntil(null);
        task.setLastFinishedAt(now);

        if (!retryable || nextAttempt >= task.getMaxAttempts()) {
            task.setStatus(ScheduledTaskStatus.FAILED);
            return;
        }

        // Retry is scheduled before the next cron occurrence so transient failures
        // get a chance to recover quickly without losing the original task.
        task.setStatus(ScheduledTaskStatus.ACTIVE);
        task.setNextRunAt(now.plus(retryDelay(nextAttempt)));
    }

    private Instant nextRunAt(String cronExpression, Instant from) {
        ZonedDateTime next = CronExpression.parse(cronExpression).next(ZonedDateTime.ofInstant(from, SCHEDULER_ZONE));
        if (next == null) {
            throw new IllegalArgumentException("Cron expression does not produce a next run time");
        }
        return next.toInstant();
    }

    private Duration retryDelay(int attempt) {
        int index = Math.min(attempt - 1, RETRY_DELAYS.size() - 1);
        return RETRY_DELAYS.get(index);
    }

    private String stackTraceOf(Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private String resolveLockOwner() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException ex) {
            return "unknown-" + UUID.randomUUID();
        }
    }
}
