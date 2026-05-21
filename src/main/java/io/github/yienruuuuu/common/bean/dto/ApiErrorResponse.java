package io.github.yienruuuuu.common.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "API 錯誤回應格式")
public record ApiErrorResponse(
        @Schema(description = "系統錯誤碼", example = "2001")
        Integer code,

        @Schema(description = "錯誤碼名稱", example = "INVALID_ARGUMENT")
        String name,

        @Schema(description = "錯誤訊息", example = "Invalid cron expression")
        String message,

        @Schema(description = "錯誤發生時間", example = "2026-05-21T08:00:00Z")
        Instant timestamp
) {
}
