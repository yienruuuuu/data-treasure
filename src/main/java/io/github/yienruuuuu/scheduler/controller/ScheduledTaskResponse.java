package io.github.yienruuuuu.scheduler.controller;

import io.github.yienruuuuu.scheduler.domain.ScheduledTaskStatus;
import io.github.yienruuuuu.scheduler.entity.ScheduledTaskEntity;

import java.time.Instant;
import java.util.UUID;

public record ScheduledTaskResponse(
        UUID id,
        String taskType,
        String cronExpression,
        String payload,
        ScheduledTaskStatus status,
        Instant nextRunAt,
        int attempt,
        int maxAttempts,
        String lockOwner,
        Instant lockUntil,
        Instant lastStartedAt,
        Instant lastFinishedAt,
        Instant createdAt,
        Instant updatedAt
) {
    static ScheduledTaskResponse from(ScheduledTaskEntity entity) {
        return new ScheduledTaskResponse(
                entity.getId(),
                entity.getTaskType(),
                entity.getCronExpression(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getNextRunAt(),
                entity.getAttempt(),
                entity.getMaxAttempts(),
                entity.getLockOwner(),
                entity.getLockUntil(),
                entity.getLastStartedAt(),
                entity.getLastFinishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
