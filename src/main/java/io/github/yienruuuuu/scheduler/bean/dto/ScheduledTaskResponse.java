package io.github.yienruuuuu.scheduler.bean.dto;

import io.github.yienruuuuu.scheduler.domain.ScheduledTaskStatus;
import io.github.yienruuuuu.scheduler.bean.po.ScheduledTaskEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "排程任務狀態回應")
public record ScheduledTaskResponse(
        @Schema(description = "任務 ID", example = "018f9f2c-2f6a-7b4b-9a4b-6f2b0c5f4d11")
        UUID id,

        @Schema(description = "任務類型", example = "DATA_RESEARCH")
        String taskType,

        @Schema(description = "Cron 表達式", example = "0 */5 * * * *")
        String cronExpression,

        @Schema(description = "任務 payload JSON 字串", example = "{\"subject\":\"scheduler framework\",\"depth\":\"deep\",\"fail\":false}")
        String payload,

        @Schema(description = "任務狀態", example = "ACTIVE")
        ScheduledTaskStatus status,

        @Schema(description = "下一次執行時間", example = "2026-05-21T08:05:00Z")
        Instant nextRunAt,

        @Schema(description = "目前已連續失敗或嘗試次數", example = "0")
        int attempt,

        @Schema(description = "最大嘗試次數", example = "3")
        int maxAttempts,

        @Schema(description = "目前 claim 任務的應用實例識別；未執行時為 null", example = "worker-01-018f9f2c")
        String lockOwner,

        @Schema(description = "任務鎖定到期時間；未執行時為 null", example = "2026-05-21T08:10:00Z")
        Instant lockUntil,

        @Schema(description = "最近一次開始執行時間", example = "2026-05-21T08:00:00Z")
        Instant lastStartedAt,

        @Schema(description = "最近一次執行完成時間", example = "2026-05-21T08:00:03Z")
        Instant lastFinishedAt,

        @Schema(description = "建立時間", example = "2026-05-21T07:59:00Z")
        Instant createdAt,

        @Schema(description = "更新時間", example = "2026-05-21T08:00:03Z")
        Instant updatedAt
) {
    public static ScheduledTaskResponse from(ScheduledTaskEntity entity) {
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
