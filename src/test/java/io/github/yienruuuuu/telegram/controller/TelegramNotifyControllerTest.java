package io.github.yienruuuuu.telegram.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.yienruuuuu.common.controller.GlobalExceptionHandler;
import io.github.yienruuuuu.common.exception.BadRequestApiException;
import io.github.yienruuuuu.common.error.SysCode;
import io.github.yienruuuuu.telegram.bean.dto.TelegramSendRequest;
import io.github.yienruuuuu.telegram.service.TelegramBotClient;
import io.github.yienruuuuu.telegram.service.TelegramNotifyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelegramNotifyController.class)
@Import(GlobalExceptionHandler.class)
class TelegramNotifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TelegramNotifyService telegramNotifyService;

    @Test
    void sendReturnsOkWhenRequestValid() throws Exception {
        // given
        when(telegramNotifyService.sendText(anyString()))
                .thenReturn(new TelegramBotClient.TelegramSendResult(true, "-1001", 88, null));

        TelegramSendRequest request = new TelegramSendRequest(null, "hello");

        // when / then
        mockMvc.perform(post("/api/telegram/notify/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.chatId").value("-1001"))
                .andExpect(jsonPath("$.messageId").value(88));
    }

    @Test
    void sendReturnsBadRequestWhenMessageBlank() throws Exception {
        // given
        TelegramSendRequest request = new TelegramSendRequest(null, "");

        // when / then
        mockMvc.perform(post("/api/telegram/notify/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("INVALID_ARGUMENT"));
    }

    @Test
    void sendReturnsBadRequestWhenServiceThrowsApiException() throws Exception {
        // given
        when(telegramNotifyService.sendText(anyString()))
                .thenThrow(new BadRequestApiException(SysCode.INVALID_ARGUMENT, "bad request"));

        TelegramSendRequest request = new TelegramSendRequest(null, "hello");

        // when / then
        mockMvc.perform(post("/api/telegram/notify/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("INVALID_ARGUMENT"));
    }
}
