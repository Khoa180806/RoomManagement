package com.khoa.roommanagement.reminders.service;

import com.khoa.roommanagement.reminders.telegram.TelegramApiClient;
import com.khoa.roommanagement.reminders.telegram.TelegramNotConfiguredException;
import com.khoa.roommanagement.reminders.telegram.TelegramProperties;
import org.springframework.stereotype.Service;

@Service
public class TelegramTestMessageService {

    private static final String TEST_MESSAGE = """
        🔔 Tin nhắn thử từ ứng dụng Quản lý phòng trọ.

        Nếu bạn nhận được tin này, cấu hình Telegram đã hoạt động đúng.\
        Các nhắc hạn thanh toán sẽ được gửi đến chat này.""";

    private final TelegramApiClient apiClient;
    private final TelegramProperties properties;

    public TelegramTestMessageService(TelegramApiClient apiClient, TelegramProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
    }

    /**
     * Gửi một tin nhắn thử đến chat ID đã cấu hình qua biến môi trường.
     * Client không thể ghi hoặc đọc bot token/chat ID qua API này.
     */
    public void sendTestMessage() {
        if (!properties.isConfigured()) {
            throw new TelegramNotConfiguredException();
        }
        apiClient.sendMessage(properties.chatId(), TEST_MESSAGE);
    }
}
