package io.github.yienruuuuu.xtracker.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record XTrackerBackfillJobRequest(
        @Schema(description = "來源平台", example = "X")
        String platform,

        @Schema(description = "人物 handle", example = "elonmusk")
        String handle,

        @Schema(description = "歷史資料起始時間；未提供時會向 XTracker 按月往前探測最早可用資料時間", example = "2025-10-31T00:17:35Z")
        Instant earliestAt,

        @Schema(description = "Backfill 完成後是否啟用每五分鐘即時探測", example = "true")
        Boolean enableRealtimeAfterBackfill
) {
}
