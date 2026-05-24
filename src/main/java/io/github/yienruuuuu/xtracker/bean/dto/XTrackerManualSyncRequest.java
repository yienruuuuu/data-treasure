package io.github.yienruuuuu.xtracker.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record XTrackerManualSyncRequest(
        @Schema(description = "來源平台", example = "X")
        String platform,

        @Schema(description = "即使 API 回應未變也保存 raw snapshot", example = "false")
        Boolean forceRawSnapshot,

        @Schema(description = "抓取起始時間，含此時間", example = "2026-05-15T04:00:00Z")
        Instant startDate,

        @Schema(description = "抓取結束時間，不含此時間", example = "2026-05-22T04:00:00Z")
        Instant endDate,

        @Schema(description = "日期解讀時區。startDate 或 endDate 使用絕對時間時不會轉傳給 XTracker。", example = "EST")
        String timezone
) {
}
