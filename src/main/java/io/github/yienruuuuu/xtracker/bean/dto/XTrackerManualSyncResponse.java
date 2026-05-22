package io.github.yienruuuuu.xtracker.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record XTrackerManualSyncResponse(
        @Schema(description = "來源平台", example = "X")
        String platform,

        @Schema(description = "被爬人物 handle", example = "elonmusk")
        String handle,

        @Schema(description = "是否新建人物主檔", example = "false")
        boolean createdPerson,

        @Schema(description = "是否建立 raw API snapshot", example = "true")
        boolean rawSnapshotCreated,

        @Schema(description = "此次 API 回應是否未變", example = "false")
        boolean unchanged,

        @Schema(description = "解析到的 post 數量", example = "20")
        int postsFetched,

        @Schema(description = "新增 post 數量", example = "3")
        int postsInserted,

        @Schema(description = "更新 post 數量", example = "1")
        int postsUpdated,

        @Schema(description = "觀測時間")
        Instant observedAt,

        @Schema(description = "耗時毫秒", example = "842")
        long elapsedMillis
) {
}
