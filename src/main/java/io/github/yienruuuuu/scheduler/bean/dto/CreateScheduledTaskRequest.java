package io.github.yienruuuuu.scheduler.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "建立 Cron 排程任務的請求")
public record CreateScheduledTaskRequest(
        @Schema(
                description = "任務 payload，通常為 JSON 字串，由對應 handler 自行解析",
                example = "{\"sourceUrl\":\"https://arena.ai/leaderboard/text\"}"
        )
        String payload,

        @Schema(description = "最大嘗試次數，未填時預設為 3", example = "3", minimum = "1", maximum = "100")
        @Min(1)
        @Max(100)
        Integer maxAttempts
) {
}
