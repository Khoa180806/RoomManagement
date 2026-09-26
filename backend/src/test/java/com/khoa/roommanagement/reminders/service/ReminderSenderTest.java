package com.khoa.roommanagement.reminders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderChannel;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderStatus;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderType;
import com.khoa.roommanagement.reminders.reminder.repository.ReminderRepository;
import com.khoa.roommanagement.reminders.telegram.TelegramApiClient;
import com.khoa.roommanagement.reminders.telegram.TelegramProperties;
import com.khoa.roommanagement.reminders.telegram.TelegramSendException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderSenderTest {

	private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final LocalDate TARGET = LocalDate.of(2026, 9, 26);

	@Mock
	private ReminderRepository reminderRepository;

	@Mock
	private TelegramApiClient telegramApiClient;

	@Test
	void sendsScheduledReminderAndStoresMessage() {
		ReminderSender sender = senderAt(TARGET);
		when(reminderRepository.findByReminderTypeAndReferenceIdAndTargetDateAndChannel(
			any(), any(), any(), any())).thenReturn(Optional.empty());
		when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		sender.dispatchScheduled(ReminderType.BILL_UPCOMING, UUID.randomUUID(), TARGET, "Nội dung tin nhắn");

		verify(telegramApiClient).sendMessage(eq("chat-id"), eq("Nội dung tin nhắn"));
		ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ReminderStatus.SENT);
		assertThat(captor.getValue().getMessage()).isEqualTo("Nội dung tin nhắn");
		assertThat(captor.getValue().getAttemptCount()).isEqualTo(1);
	}

	@Test
	void skipsAlreadySentReminder() {
		ReminderSender sender = senderAt(TARGET);
		Reminder sent = sentReminder();
		when(reminderRepository.findByReminderTypeAndReferenceIdAndTargetDateAndChannel(
			any(), any(), any(), any())).thenReturn(Optional.of(sent));

		sender.dispatchScheduled(ReminderType.BILL_UPCOMING, sent.getReferenceId(), TARGET, "msg");

		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
		verify(reminderRepository, never()).save(any());
	}

	@Test
	void retriesSameDayFailedReminder() {
		ReminderSender sender = senderAt(TARGET);
		Reminder failed = failedReminder(1);
		when(reminderRepository.findByReminderTypeAndReferenceIdAndTargetDateAndChannel(
			any(), any(), any(), any())).thenReturn(Optional.of(failed));
		when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		sender.dispatchScheduled(ReminderType.BILL_UPCOMING, failed.getReferenceId(), TARGET, "msg");

		ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(captor.capture());
		assertThat(captor.getValue().getAttemptCount()).isEqualTo(2);
		assertThat(captor.getValue().getStatus()).isEqualTo(ReminderStatus.SENT);
	}

	@Test
	void retriesFailedReminderAfterBackoffElapsed() {
		// attemptCount=2 → lần kế tiếp phải chờ tới targetDate + 2 ngày.
		ReminderSender sender = senderAt(TARGET.plusDays(1));
		Reminder failed = failedReminder(2);
		when(reminderRepository.findById(failed.getId())).thenReturn(Optional.of(failed));

		sender.dispatchRetry(failed.getId());

		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
		verify(reminderRepository, never()).save(any());
	}

	@Test
	void retriesAfterBackoffDay() {
		ReminderSender sender = senderAt(TARGET.plusDays(2));
		Reminder failed = failedReminder(2);
		when(reminderRepository.findById(failed.getId())).thenReturn(Optional.of(failed));
		when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		sender.dispatchRetry(failed.getId());

		ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(captor.capture());
		assertThat(captor.getValue().getAttemptCount()).isEqualTo(3);
		assertThat(captor.getValue().getStatus()).isEqualTo(ReminderStatus.SENT);
	}

	@Test
	void stopsAfterMaxAttempts() {
		ReminderSender sender = senderAt(TARGET.plusDays(2));
		Reminder exhausted = failedReminder(Reminder.MAX_ATTEMPTS);
		when(reminderRepository.findById(exhausted.getId())).thenReturn(Optional.of(exhausted));

		sender.dispatchRetry(exhausted.getId());

		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
		verify(reminderRepository, never()).save(any());
	}

	@Test
	void recordsFailedWhenTelegramRejects() {
		ReminderSender sender = senderAt(TARGET);
		when(reminderRepository.findByReminderTypeAndReferenceIdAndTargetDateAndChannel(
			any(), any(), any(), any())).thenReturn(Optional.empty());
		when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		org.mockito.Mockito.doThrow(new TelegramSendException("Telegram từ chối yêu cầu"))
			.when(telegramApiClient).sendMessage(anyString(), anyString());

		sender.dispatchScheduled(ReminderType.BILL_UPCOMING, UUID.randomUUID(), TARGET, "msg");

		ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ReminderStatus.FAILED);
		assertThat(captor.getValue().getErrorCode()).isEqualTo("TELEGRAM_SEND_FAILED");
		assertThat(captor.getValue().canRetry(TARGET)).isTrue();
	}

	@Test
	void recordsFailedWhenNotConfigured() {
		ReminderSender sender = new ReminderSender(reminderRepository, telegramApiClient,
			new TelegramProperties("", "", "https://api.telegram.org"),
			Clock.fixed(TARGET.atTime(LocalTime.NOON).atZone(VIETNAM).toInstant(), VIETNAM));
		when(reminderRepository.findByReminderTypeAndReferenceIdAndTargetDateAndChannel(
			any(), any(), any(), any())).thenReturn(Optional.empty());
		when(reminderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		sender.dispatchScheduled(ReminderType.BILL_UPCOMING, UUID.randomUUID(), TARGET, "msg");

		ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ReminderStatus.FAILED);
		assertThat(captor.getValue().getErrorCode()).isEqualTo("TELEGRAM_NOT_CONFIGURED");
	}

	private ReminderSender senderAt(LocalDate date) {
		return new ReminderSender(reminderRepository, telegramApiClient,
			new TelegramProperties("token", "chat-id", "https://api.telegram.org"),
			Clock.fixed(date.atTime(LocalTime.NOON).atZone(VIETNAM).toInstant(), VIETNAM));
	}

	private Reminder sentReminder() {
		Reminder reminder = Reminder.create(ReminderType.BILL_UPCOMING, UUID.randomUUID(),
			TARGET, ReminderChannel.TELEGRAM, "msg");
		reminder.markAttempt();
		reminder.markSent(java.time.Instant.now());
		return reminder;
	}

	private Reminder failedReminder(int attempts) {
		Reminder reminder = Reminder.create(ReminderType.BILL_UPCOMING, UUID.randomUUID(),
			TARGET, ReminderChannel.TELEGRAM, "msg");
		for (int i = 0; i < attempts; i++) {
			reminder.markAttempt();
			reminder.markFailed("TELEGRAM_SEND_FAILED");
		}
		return reminder;
	}
}
