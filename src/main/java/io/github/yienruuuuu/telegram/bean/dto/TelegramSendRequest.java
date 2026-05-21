package io.github.yienruuuuu.telegram.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Telegram 手動發送通知請求")
public record TelegramSendRequest(
        @Schema(description = "目標 chat id；未填則使用預設 DEFAULT_CHAT_ID", example = "-1001234567890")
        String chatId,

        @Schema(description = "通知訊息內容", example = "Arena crawl partial result: declared=360, fetched=358")
        @NotBlank
        String message
) {
}
