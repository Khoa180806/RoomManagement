package com.khoa.roommanagement.reminders.service;

import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderChannel;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderStatus;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderType;
import com.khoa.roommanagement.reminders.reminder.repository.ReminderRepository;
import com.khoa.roommanagement.reminders.telegram.TelegramApiClient;
import com.khoa.roommanagement.reminders.telegram.TelegramProperties;
import com.khoa.roommanagement.reminders.telegram.TelegramSendException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gửi một reminder trong transaction riêng của nó: một lần gửi lỗi không
 * được ảnh hưởng đến các reminder khác trong ngày, và dòng SENT được cam
 * kết ngay sau khi Telegram xác nhận.
 */
@Service
public class ReminderSender {

	private final ReminderRepository reminderRepository;
	private final TelegramApiClient telegramApiClient;
	private final TelegramProperties telegramProperties;
	private final Clock clock;

	public ReminderSender(
		ReminderRepository reminderRepository,
		TelegramApiClient telegramApiClient,
		TelegramProperties telegramProperties,
		Clock clock
	) {
		this.reminderRepository = reminderRepository;
		this.telegramApiClient = telegramApiClient;
		this.telegramProperties = telegramProperties;
		this.clock = clock;
	}

	/** Nhắc theo lịch: lấy bản ghi cũ nếu có (retry cùng ngày) hoặc tạo mới. */
	@Transactional
	public void dispatchScheduled(ReminderType type, UUID referenceId, LocalDate targetDate, String message) {
		Reminder reminder = reminderRepository
			.findByReminderTypeAndReferenceIdAndTargetDateAndChannel(
				type, referenceId, targetDate, ReminderChannel.TELEGRAM)
			.orElseGet(() -> Reminder.create(type, referenceId, targetDate, ReminderChannel.TELEGRAM, message));
		reminder.setMessage(message);
		attempt(reminder);
	}

	/** Retry một reminder FAILED/PENDING trong cửa sổ retry. */
	@Transactional
	public void dispatchRetry(UUID reminderId) {
		reminderRepository.findById(reminderId).ifPresent(this::attempt);
	}

	private void attempt(Reminder reminder) {
		LocalDate today = LocalDate.now(clock);
		if (reminder.getStatus() == ReminderStatus.SENT) {
			return;
		}
		if (reminder.getAttemptCount() >= Reminder.MAX_ATTEMPTS) {
			return;
		}
		if (!reminder.canRetry(today)) {
			return;
		}

		reminder.markAttempt();
		if (!telegramProperties.isConfigured()) {
			reminder.markFailed("TELEGRAM_NOT_CONFIGURED");
			reminderRepository.save(reminder);
			return;
		}

		try {
			telegramApiClient.sendMessage(telegramProperties.chatId(), reminder.getMessage());
			reminder.markSent(Instant.now(clock));
		} catch (TelegramSendException exception) {
			reminder.markFailed(exception.getSafeErrorCode());
		}
		reminderRepository.save(reminder);
	}
}
