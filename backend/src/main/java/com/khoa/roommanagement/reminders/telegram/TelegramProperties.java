package com.khoa.roommanagement.reminders.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình Telegram chỉ nhận giá trị từ biến môi trường. Token và chat ID
 * không được ghi log, trả qua API hay commit vào repository.
 */
@ConfigurationProperties(prefix = "app.telegram")
public record TelegramProperties(
    String botToken,
    String chatId,
    @DefaultValue("https://api.telegram.org") String apiBaseUrl
) {

    public static final String DEFAULT_API_BASE_URL = "https://api.telegram.org";

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank()
            && chatId != null && !chatId.isBlank();
    }
}
