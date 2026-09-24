package com.khoa.roommanagement.reminders.telegram;

public class TelegramNotConfiguredException extends RuntimeException {

    public TelegramNotConfiguredException() {
        super("Chưa cấu hình Telegram. Hãy đặt TELEGRAM_BOT_TOKEN và TELEGRAM_CHAT_ID trong biến môi trường.");
    }
}
