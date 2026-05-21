package io.github.yienruuuuu.telegram.service;

import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.common.exception.InternalApiException;
import io.github.yienruuuuu.scheduler.service.ArenaTextOverallIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for Telegram text notifications.
 */
@Slf4j
@Service
public class TelegramNotifyService {

    private final TelegramBotClient telegramBotClient;
    private final String botToken;
    private final String defaultChatId;

    public TelegramNotifyService(
            TelegramBotClient telegramBotClient,
            @Value("${telegram.bot.token:${BOT_TOKEN:}}") String botToken,
            @Value("${telegram.default-chat-id:${DEFAULT_CHAT_ID:}}") String defaultChatId
    ) {
        this.telegramBotClient = telegramBotClient;
        this.botToken = botToken;
        this.defaultChatId = defaultChatId;
    }

    public TelegramBotClient.TelegramSendResult sendText(String message) {
        return sendText(defaultChatId, message);
    }

    public TelegramBotClient.TelegramSendResult sendText(String chatId, String message) {
        if (message == null || message.isBlank()) {
            throw new BadRequestApiException(SysCode.INVALID_ARGUMENT, "message must not be blank");
        }
        if (botToken == null || botToken.isBlank()) {
            throw new InternalApiException(SysCode.INTERNAL_ERROR, "BOT_TOKEN is not configured", null);
        }
        if (chatId == null || chatId.isBlank()) {
            throw new InternalApiException(SysCode.INTERNAL_ERROR, "DEFAULT_CHAT_ID is not configured", null);
        }

        TelegramBotClient.TelegramSendResult result = telegramBotClient.sendMessage(botToken, chatId, message);
        if (!result.success()) {
            throw new InternalApiException(
                    SysCode.INTERNAL_ERROR,
                    "Failed to send Telegram message: " + result.errorMessage(),
                    null
            );
        }
        log.info("Telegram notification sent. chatId={}, messageId={}", result.chatId(), result.messageId());
        return result;
    }

    public void sendCrawlAnomaly(ArenaTextOverallIngestionService.CrawlAnomalyContext context) {
        String message = """
                [DataTreasure] Arena crawl partial result
                leaderboardKey: %s
                sourceUrl: %s
                updatedDate: %s
                declared: %d
                fetched: %d
                """.formatted(
                context.leaderboardKey(),
                context.sourceUrl(),
                context.updatedDate(),
                context.declaredModelCount(),
                context.fetchedModelCount()
        );
        sendText(message);
    }
}
