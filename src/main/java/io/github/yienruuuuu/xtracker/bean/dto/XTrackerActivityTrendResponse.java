package io.github.yienruuuuu.xtracker.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "XTracker 人物發文趨勢圖表資料")
public record XTrackerActivityTrendResponse(
        @Schema(description = "資料源資訊")
        Source source,

        @Schema(description = "查詢時間區間與聚合設定")
        Range range,

        @Schema(description = "圖表彙總指標")
        Metrics metrics,

        @Schema(description = "依 bucket 聚合的發文量與累積量")
        List<SeriesPoint> series,

        @Schema(description = "趨勢摘要")
        Insight insight
) {
    public record Source(
            @Schema(description = "來源平台", example = "X")
            String platform,

            @Schema(description = "資料源標籤", example = "Tweet")
            String sourceLabel,

            @Schema(description = "前端人物識別碼", example = "elon-musk")
            String personId,

            @Schema(description = "X handle", example = "elonmusk")
            String handle,

            @Schema(description = "顯示名稱", example = "Elon Musk")
            String displayName
    ) {
    }

    public record Range(
            @Schema(description = "查詢起始 UTC Instant，含此時間", example = "2026-05-23T16:00:00Z")
            Instant startAt,

            @Schema(description = "查詢結束 UTC Instant，不含此時間", example = "2026-05-24T13:00:00Z")
            Instant endAt,

            @Schema(description = "回應時間基準，固定為 UTC", example = "UTC")
            String timezone,

            @Schema(description = "聚合 bucket", example = "hour")
            String bucket
    ) {
    }

    public record Metrics(
            @Schema(description = "查詢區間內總發文數", example = "609")
            long totalCount,

            @Schema(description = "區間內峰值 bucket 起始 UTC Instant，可為 null", example = "2026-05-24T04:00:00Z")
            Instant peakBucketStartAt,

            @Schema(description = "區間內峰值 bucket 發文數", example = "225")
            long peakBucketCount,

            @Schema(description = "截至查詢結束時間的歷史累積發文數", example = "9600")
            long cumulativeEndCount
    ) {
    }

    public record SeriesPoint(
            @Schema(description = "bucket 起始 UTC Instant，含此時間", example = "2026-05-23T16:00:00Z")
            Instant bucketStartAt,

            @Schema(description = "bucket 結束 UTC Instant，不含此時間", example = "2026-05-23T17:00:00Z")
            Instant bucketEndAt,

            @Schema(description = "該 bucket 發文數；保留 dailyCount 名稱以對齊前端圖表", example = "35")
            long dailyCount,

            @Schema(description = "截至 bucketEndAt 的歷史累積發文數", example = "700")
            long cumulativeCount
    ) {
    }

    public record Insight(
            @Schema(description = "摘要標題", example = "趨勢摘要")
            String title,

            @Schema(description = "摘要內容", example = "區間內出現明顯發文高峰，累積曲線維持上升。")
            String body
    ) {
    }
}
