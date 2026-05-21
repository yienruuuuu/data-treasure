package io.github.yienruuuuu.scheduler.bean.dto;

import io.github.yienruuuuu.scheduler.domain.ScheduledTaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "建立 Cron 排程任務的請求")
public record CreateScheduledTaskRequest(
        @Schema(description = "任務類型，必須是系統已定義的 enum 常數", example = "DATA_RESEARCH")
        @NotNull
        ScheduledTaskType taskType,

        @Schema(
                description = "Spring Cron 表達式，格式為：秒 分 時 日 月 星期",
                example = "0 */5 * * * *"
        )
        @NotBlank
        String cronExpression,

        @Schema(
                description = "任務 payload，通常為 JSON 字串，由對應 handler 自行解析",
                example = "{\"subject\":\"scheduler framework\",\"depth\":\"deep\",\"fail\":false}"
        )
        String payload,

        @Schema(description = "最大嘗試次數，未填時預設為 3", example = "3", minimum = "1", maximum = "100")
        @Min(1)
        @Max(100)
        Integer maxAttempts
) {
}
