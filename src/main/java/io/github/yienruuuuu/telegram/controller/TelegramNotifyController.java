package io.github.yienruuuuu.telegram.controller;

import io.github.yienruuuuu.common.bean.dto.ApiErrorResponse;
import io.github.yienruuuuu.telegram.bean.dto.TelegramSendRequest;
import io.github.yienruuuuu.telegram.bean.dto.TelegramSendResponse;
import io.github.yienruuuuu.telegram.service.TelegramBotClient;
import io.github.yienruuuuu.telegram.service.TelegramNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manual Telegram notification endpoint.
 */
@Slf4j
@Tag(name = "Telegram 通知", description = "手動發送 Telegram 通知訊息。")
@RestController
@RequestMapping("/api/telegram/notify")
public class TelegramNotifyController {

    private final TelegramNotifyService telegramNotifyService;

    public TelegramNotifyController(TelegramNotifyService telegramNotifyService) {
        this.telegramNotifyService = telegramNotifyService;
    }

    @Operation(
            summary = "發送 Telegram 文字通知",
            description = "發送一則 Telegram 純文字訊息。未提供 chatId 時使用 DEFAULT_CHAT_ID。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "發送成功"),
                    @ApiResponse(responseCode = "400", description = "請求參數錯誤",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "發送失敗或設定缺失",
                            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    @PostMapping("/send")
    public TelegramSendResponse send(@Valid @RequestBody TelegramSendRequest request) {
        log.info("Manual telegram notify requested. chatId={}", request.chatId());
        TelegramBotClient.TelegramSendResult result = request.chatId() == null || request.chatId().isBlank()
                ? telegramNotifyService.sendText(request.message())
                : telegramNotifyService.sendText(request.chatId(), request.message());
        return new TelegramSendResponse(true, result.chatId(), result.messageId());
    }
}
