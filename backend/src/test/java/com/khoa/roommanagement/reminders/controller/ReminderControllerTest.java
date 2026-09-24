package com.khoa.roommanagement.reminders.controller;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khoa.roommanagement.common.exception.ApiExceptionHandler;
import com.khoa.roommanagement.reminders.service.TelegramTestMessageService;
import com.khoa.roommanagement.reminders.telegram.TelegramNotConfiguredException;
import com.khoa.roommanagement.reminders.telegram.TelegramSendException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReminderController.class)
@Import(ApiExceptionHandler.class)
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelegramTestMessageService telegramTestMessageService;

    @Test
    void sendsTestMessageSuccessfully() throws Exception {
        mockMvc.perform(post("/api/reminders/test")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sent").value(true));
    }

    @Test
    void returnsConflictWhenTelegramNotConfigured() throws Exception {
        doThrow(new TelegramNotConfiguredException()).when(telegramTestMessageService).sendTestMessage();

        mockMvc.perform(post("/api/reminders/test")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("TELEGRAM_NOT_CONFIGURED"));
    }

    @Test
    void returnsBadGatewayWhenTelegramFails() throws Exception {
        doThrow(new TelegramSendException("Telegram từ chối yêu cầu (mã 400): chat not found"))
            .when(telegramTestMessageService).sendTestMessage();

        mockMvc.perform(post("/api/reminders/test")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error.code").value("TELEGRAM_SEND_FAILED"));
    }
}
