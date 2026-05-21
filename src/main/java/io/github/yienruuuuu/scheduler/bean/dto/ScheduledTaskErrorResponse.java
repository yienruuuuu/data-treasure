package io.github.yienruuuuu.scheduler.bean.dto;

import io.github.yienruuuuu.scheduler.bean.po.ScheduledTaskErrorEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "排程任務錯誤紀錄回應")
public record ScheduledTaskErrorResponse(
        @Schema(description = "錯誤紀錄 ID", example = "018f9f2c-2f6a-7b4b-9a4b-6f2b0c5f4d12")
        UUID id,

        @Schema(description = "對應的任務 ID", example = "018f9f2c-2f6a-7b4b-9a4b-6f2b0c5f4d11")
        UUID taskId,

        @Schema(description = "失敗發生時的嘗試次數", example = "1")
        int attempt,

        @Schema(description = "例外類型", example = "java.lang.IllegalStateException")
        String errorType,

        @Schema(description = "例外訊息", example = "Demo data research task failed by payload flag")
        String errorMessage,

        @Schema(description = "完整 stack trace，方便開發或營運排查")
        String stackTrace,

        @Schema(description = "錯誤發生時間", example = "2026-05-21T08:00:03Z")
        Instant createdAt
) {
    public static ScheduledTaskErrorResponse from(ScheduledTaskErrorEntity entity) {
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
