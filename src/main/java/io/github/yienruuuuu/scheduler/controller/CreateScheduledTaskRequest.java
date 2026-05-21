package io.github.yienruuuuu.scheduler.controller;

import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateScheduledTaskRequest(
        @NotNull
        ScheduledTaskType taskType,

        @NotBlank
        String cronExpression,

        String payload,

        @Min(1)
        @Max(100)
        Integer maxAttempts
) {
}
