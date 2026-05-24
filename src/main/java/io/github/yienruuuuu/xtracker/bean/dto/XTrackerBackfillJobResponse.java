package io.github.yienruuuuu.xtracker.bean.dto;

import io.github.yienruuuuu.xtracker.bean.po.XTrackerBackfillJobEntity;
import io.github.yienruuuuu.xtracker.domain.XTrackerBackfillStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record XTrackerBackfillJobResponse(
        @Schema(description = "Backfill job id")
        UUID id,
        String platform,
        String handle,
        Instant earliestAt,
        Instant cutoffAt,
        boolean enableRealtimeAfter,
        UUID realtimeTaskId,
        XTrackerBackfillStatus status,
        int totalWindows,
        int completedWindows,
        int failedWindows,
        String errorMessage,
        Instant createdAt,
        Instant completedAt
) {

    public static XTrackerBackfillJobResponse from(XTrackerBackfillJobEntity entity) {
        return new XTrackerBackfillJobResponse(
                entity.getId(),
                entity.getPlatform(),
                entity.getHandle(),
                entity.getEarliestAt(),
                entity.getCutoffAt(),
                entity.isEnableRealtimeAfter(),
                entity.getRealtimeTaskId(),
                entity.getStatus(),
                entity.getTotalWindows(),
                entity.getCompletedWindows(),
                entity.getFailedWindows(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
