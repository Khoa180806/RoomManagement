package com.khoa.roommanagement.reminders.telegram;

/**
 * Thông báo lỗi an toàn khi gửi Telegram: không chứa bot token, URL thật
 * hay nội dung lỗi gốc của HTTP client. Chỉ mang mã lỗi an toàn để ghi
 * vào lịch sử reminder.
 */
public class TelegramSendException extends RuntimeException {

	private final String safeErrorCode;

	public TelegramSendException(String safeMessage) {
		super(safeMessage);
		this.safeErrorCode = "TELEGRAM_SEND_FAILED";
	}

	public TelegramSendException(String safeMessage, String safeErrorCode) {
		super(safeMessage);
		this.safeErrorCode = safeErrorCode;
	}

	public String getSafeErrorCode() {
		return safeErrorCode;
	}
}
