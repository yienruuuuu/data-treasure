package io.github.yienruuuuu.scheduler.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "人工觸發 Arena Text Overall 同步請求")
public record ArenaTextOverallManualSyncRequest(
        @Schema(description = "來源網址，未填時使用預設排行榜網址", example = "https://arena.ai/leaderboard/text")
        String sourceUrl
) {
}
