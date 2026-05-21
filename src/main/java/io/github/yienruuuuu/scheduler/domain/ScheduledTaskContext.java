package io.github.yienruuuuu.scheduler.domain;

import java.util.UUID;

public record ScheduledTaskContext(
        UUID taskId,
        ScheduledTaskType taskType,
        String payload,
        int attempt
) {
}
