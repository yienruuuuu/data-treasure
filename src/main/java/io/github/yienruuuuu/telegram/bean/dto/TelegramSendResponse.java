package io.github.yienruuuuu.telegram.bean.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Telegram 手動發送通知回應")
public record TelegramSendResponse(
        @Schema(description = "是否發送成功", example = "true")
        boolean success,

        @Schema(description = "實際發送目標 chat id", example = "-1001234567890")
        String chatId,

        @Schema(description = "Telegram message id", example = "321")
        Integer messageId
) {
}
