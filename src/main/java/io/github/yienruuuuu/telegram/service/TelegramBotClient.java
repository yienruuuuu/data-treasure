package io.github.yienruuuuu.telegram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Telegram Bot API client used only for sending text messages.
 */
@Slf4j
@Component
public class TelegramBotClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TelegramBotClient(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public TelegramSendResult sendMessage(String botToken, String chatId, String message) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        try {
            RequestEntity<Map<String, String>> request = RequestEntity.post(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("chat_id", chatId, "text", message));
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                return new TelegramSendResult(false, chatId, null, "Telegram response body is empty");
            }
            return parseSendResult(chatId, responseBody);
        } catch (RestClientException exception) {
            log.error("Telegram sendMessage request failed. chatId={}", chatId, exception);
            return new TelegramSendResult(false, chatId, null, exception.getMessage());
        }
    }

    private TelegramSendResult parseSendResult(String chatId, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            boolean ok = root.path("ok").asBoolean(false);
            if (!ok) {
                String description = root.path("description").asText("Unknown Telegram error");
                return new TelegramSendResult(false, chatId, null, description);
            }
            Integer messageId = root.path("result").path("message_id").isMissingNode()
                    ? null
                    : root.path("result").path("message_id").asInt();
            return new TelegramSendResult(true, chatId, messageId, null);
        } catch (Exception exception) {
            log.error("Failed to parse Telegram sendMessage response. chatId={}, body={}", chatId, responseBody, exception);
            return new TelegramSendResult(false, chatId, null, "Failed to parse Telegram response");
        }
    }

    public record TelegramSendResult(
            boolean success,
            String chatId,
            Integer messageId,
            String errorMessage
    ) {
    }
}
