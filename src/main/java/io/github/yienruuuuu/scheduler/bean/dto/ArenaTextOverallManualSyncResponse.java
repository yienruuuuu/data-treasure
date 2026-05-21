package io.github.yienruuuuu.scheduler.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "人工觸發 Arena Text Overall 同步結果")
public record ArenaTextOverallManualSyncResponse(
        @Schema(description = "排行榜識別鍵", example = "text_overall")
        String leaderboardKey,
        @Schema(description = "實際抓取來源網址", example = "https://arena.ai/leaderboard/text")
        String sourceUrl,
        @Schema(description = "排行榜更新日期", example = "2026-05-18")
        LocalDate updatedDate,
        @Schema(description = "頁面宣告的模型總數", example = "360")
        int declaredModelCount,
        @Schema(description = "實際解析到的模型筆數", example = "360")
        int fetchedModelCount
) {
    public static ArenaTextOverallManualSyncResponse from(ArenaTextOverallSnapshotData data) {
        return new ArenaTextOverallManualSyncResponse(
                data.leaderboardKey(),
                data.sourceUrl(),
                data.updatedDate(),
                data.declaredModelCount(),
                data.items().size()
        );
    }
}
