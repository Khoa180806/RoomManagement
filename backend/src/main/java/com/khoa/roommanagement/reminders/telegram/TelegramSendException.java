package com.khoa.roommanagement.reminders.telegram;

/**
 * Thông báo lỗi an toàn khi gửi Telegram: không chứa bot token, URL thật
 * hay nội dung lỗi gốc của HTTP client.
 */
public class TelegramSendException extends RuntimeException {

    public TelegramSendException(String safeMessage) {
        super(safeMessage);
    }
}
