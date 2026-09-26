package com.khoa.roommanagement.reminders.telegram;

import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra cấu hình Telegram khi ứng dụng khởi động theo SPEC-reminders.
 * Chỉ ghi trạng thái ra log — không bao giờ log giá trị token/chat ID.
 */
@Component
public class TelegramStartupValidator {

	private static final Logger log = LoggerFactory.getLogger(TelegramStartupValidator.class);

	// Định dạng token Telegram: <bot-id>:<chuỗi alphanumeric/underscore>
	private static final Pattern TOKEN_FORMAT = Pattern.compile("^\\d{6,12}:[A-Za-z0-9_-]{20,}$");

	private final TelegramProperties properties;

	public TelegramStartupValidator(TelegramProperties properties) {
		this.properties = properties;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void validate() {
		if (!properties.isConfigured()) {
			log.warn("Telegram chưa được cấu hình: đặt TELEGRAM_BOT_TOKEN và TELEGRAM_CHAT_ID "
				+ "trong biến môi trường để bật nhắc hạn.");
			return;
		}
		if (!TOKEN_FORMAT.matcher(properties.botToken()).matches()) {
			log.warn("TELEGRAM_BOT_TOKEN có định dạng bất thường; kiểm tra lại token từ @BotFather.");
			return;
		}
		log.info("Telegram đã được cấu hình cho nhắc hạn.");
	}
}
