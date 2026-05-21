package io.github.yienruuuuu.scheduler.service;

import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.common.exception.NotFoundApiException;
import io.github.yienruuuuu.scheduler.dao.ScheduledTaskDao;
import io.github.yienruuuuu.scheduler.dao.ScheduledTaskErrorDao;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskStatus;
import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import io.github.yienruuuuu.scheduler.bean.po.ScheduledTaskEntity;
import io.github.yienruuuuu.scheduler.bean.po.ScheduledTaskErrorEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Application service for creating, enabling, disabling, and querying scheduled tasks.
 */
@Service
public class ScheduledTaskService {

    private static final ZoneId SCHEDULER_ZONE = ZoneId.of("Asia/Taipei");

    private final ScheduledTaskDao scheduledTaskDao;
    private final ScheduledTaskErrorDao scheduledTaskErrorDao;

    public ScheduledTaskService(ScheduledTaskDao scheduledTaskDao, ScheduledTaskErrorDao scheduledTaskErrorDao) {
        this.scheduledTaskDao = scheduledTaskDao;
        this.scheduledTaskErrorDao = scheduledTaskErrorDao;
    }

    @Transactional
    public UUID createCronTask(ScheduledTaskType taskType, String payload, int maxAttempts) {
        String cronExpression = taskType.cronExpression();
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "Invalid cron expression");
        }
        if (maxAttempts < 1) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "maxAttempts must be greater than 0");
        }

        ScheduledTaskEntity task = new ScheduledTaskEntity();
        task.setTaskType(taskType.name());
        task.setCronExpression(cronExpression);
        task.setPayload(payload);
        task.setStatus(ScheduledTaskStatus.ACTIVE);
        task.setNextRunAt(nextRunAt(cronExpression, Instant.now()));
        task.setAttempt(0);
        task.setMaxAttempts(maxAttempts);

        return scheduledTaskDao.save(task).getId();
    }

    @Transactional
    public void enable(UUID taskId) {
        ScheduledTaskEntity task = getRequiredTask(taskId);
        task.setStatus(ScheduledTaskStatus.ACTIVE);
        task.setAttempt(0);
        task.setLockOwner(null);
        task.setLockUntil(null);
        if (task.getNextRunAt().isBefore(Instant.now())) {
            task.setNextRunAt(nextRunAt(task.getCronExpression(), Instant.now()));
        }
    }

    @Transactional
    public void disable(UUID taskId) {
        ScheduledTaskEntity task = getRequiredTask(taskId);
        task.setStatus(ScheduledTaskStatus.DISABLED);
        task.setLockOwner(null);
        task.setLockUntil(null);
    }

    @Transactional(readOnly = true)
    public ScheduledTaskEntity getTask(UUID taskId) {
        return getRequiredTask(taskId);
    }

    @Transactional(readOnly = true)
    public List<ScheduledTaskErrorEntity> getErrors(UUID taskId) {
        return scheduledTaskErrorDao.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    Instant nextRunAt(String cronExpression, Instant from) {
        ZonedDateTime next = CronExpression.parse(cronExpression).next(ZonedDateTime.ofInstant(from, SCHEDULER_ZONE));
        if (next == null) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "Cron expression does not produce a next run time");
        }
        return next.toInstant();
    }

    private ScheduledTaskEntity getRequiredTask(UUID taskId) {
        return scheduledTaskDao.findTaskById(taskId)
                .orElseThrow(() -> new NotFoundApiException(SysCode.NOT_FOUND, "Scheduled task not found: " + taskId));
    }
}
