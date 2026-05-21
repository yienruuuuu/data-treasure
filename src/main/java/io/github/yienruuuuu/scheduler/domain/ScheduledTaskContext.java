package io.github.yienruuuuu.scheduler.domain;

import java.util.UUID;

/**
 * Immutable execution context passed from the scheduler framework to a task handler.
 */
public record ScheduledTaskContext(
        UUID taskId,
        ScheduledTaskType taskType,
        String payload,
        int attempt
) {
}
