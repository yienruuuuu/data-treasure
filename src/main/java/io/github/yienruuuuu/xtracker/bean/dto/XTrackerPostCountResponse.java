package io.github.yienruuuuu.xtracker.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record XTrackerPostCountResponse(
        @Schema(description = "來源平台", example = "X")
        String platform,

        @Schema(description = "被爬人物 handle", example = "elonmusk")
        String handle,

        @Schema(description = "查詢起始時間，含此時間")
        Instant startAt,

        @Schema(description = "查詢結束時間，不含此時間")
        Instant endAt,

        @Schema(description = "區間內發文數", example = "7")
        long postCount
) {
}
