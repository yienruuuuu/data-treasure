package io.github.yienruuuuu.scheduler.controller;

import io.github.yienruuuuu.scheduler.entity.ScheduledTaskErrorEntity;

import java.time.Instant;
import java.util.UUID;

public record ScheduledTaskErrorResponse(
        UUID id,
        UUID taskId,
        int attempt,
        String errorType,
        String errorMessage,
        String stackTrace,
        Instant createdAt
) {
    static ScheduledTaskErrorResponse from(ScheduledTaskErrorEntity entity) {
        return new ScheduledTaskErrorResponse(
                entity.getId(),
                entity.getTask().getId(),
                entity.getAttempt(),
                entity.getErrorType(),
                entity.getErrorMessage(),
                entity.getStackTrace(),
                entity.getCreatedAt()
        );
    }
}
