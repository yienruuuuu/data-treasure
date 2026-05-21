package io.github.yienruuuuu.telegram.service;

import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.common.exception.InternalApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramNotifyServiceTest {

    private final TelegramBotClient telegramBotClient = mock(TelegramBotClient.class);

    @Test
    void sendTextUsesDefaultChatId() {
        // given
        TelegramNotifyService service = new TelegramNotifyService(telegramBotClient, "token", "-1001");
        when(telegramBotClient.sendMessage("token", "-1001", "hello"))
                .thenReturn(new TelegramBotClient.TelegramSendResult(true, "-1001", 100, null));

        // when
        TelegramBotClient.TelegramSendResult result = service.sendText("hello");

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.chatId()).isEqualTo("-1001");
        verify(telegramBotClient).sendMessage("token", "-1001", "hello");
    }

    @Test
    void sendTextUsesProvidedChatId() {
        // given
        TelegramNotifyService service = new TelegramNotifyService(telegramBotClient, "token", "-1001");
        when(telegramBotClient.sendMessage("token", "-2002", "hello"))
                .thenReturn(new TelegramBotClient.TelegramSendResult(true, "-2002", 101, null));

        // when
        TelegramBotClient.TelegramSendResult result = service.sendText("-2002", "hello");

        // then
        assertThat(result.success()).isTrue();
        verify(telegramBotClient).sendMessage("token", "-2002", "hello");
    }

    @Test
    void sendTextThrowsWhenMessageIsBlank() {
        // given
        TelegramNotifyService service = new TelegramNotifyService(telegramBotClient, "token", "-1001");

        // when / then
        assertThatThrownBy(() -> service.sendText("  "))
                .isInstanceOf(BadRequestApiException.class)
                .hasMessageContaining("message must not be blank");
    }

    @Test
    void sendTextThrowsWhenTokenMissing() {
        // given
        TelegramNotifyService service = new TelegramNotifyService(telegramBotClient, "", "-1001");

        // when / then
        assertThatThrownBy(() -> service.sendText("hello"))
                .isInstanceOf(InternalApiException.class)
                .hasMessageContaining("BOT_TOKEN is not configured");
    }

    @Test
    void sendTextThrowsWhenTelegramReturnsFailure() {
        // given
        TelegramNotifyService service = new TelegramNotifyService(telegramBotClient, "token", "-1001");
        when(telegramBotClient.sendMessage(eq("token"), eq("-1001"), eq("hello")))
                .thenReturn(new TelegramBotClient.TelegramSendResult(false, "-1001", null, "forbidden"));

        // when / then
        assertThatThrownBy(() -> service.sendText("hello"))
                .isInstanceOf(InternalApiException.class)
                .hasMessageContaining("Failed to send Telegram message");
    }
}
