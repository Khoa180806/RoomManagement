package com.khoa.roommanagement.reminders.telegram;

/**
 * Adapter gửi tin nhắn Telegram. Nằm sau interface riêng để scheduler/service
 * không phụ thuộc HTTP và có thể mock trong test.
 */
public interface TelegramApiClient {

    /**
     * Gửi một tin nhắn văn bản đến chat ID chỉ định.
     *
     * @throws TelegramSendException khi Telegram từ chối hoặc kết nối thất bại
     */
    void sendMessage(String chatId, String text);
}
