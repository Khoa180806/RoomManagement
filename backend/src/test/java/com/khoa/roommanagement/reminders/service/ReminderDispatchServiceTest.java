package com.khoa.roommanagement.reminders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.entity.BillStatus;
import com.khoa.roommanagement.billing.bills.repository.BillRepository;
import com.khoa.roommanagement.billing.contracts.dto.CreateRentalContractCommand;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderChannel;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderStatus;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderType;
import com.khoa.roommanagement.reminders.reminder.repository.ReminderRepository;
import com.khoa.roommanagement.reminders.settings.entity.ReminderSettings;
import com.khoa.roommanagement.reminders.settings.repository.ReminderSettingsRepository;
import com.khoa.roommanagement.reminders.telegram.TelegramApiClient;
import com.khoa.roommanagement.reminders.telegram.TelegramProperties;
import com.khoa.roommanagement.reminders.telegram.TelegramSendException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderDispatchServiceTest {

	private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

	@Mock
	private ReminderRepository reminderRepository;

	@Mock
	private ReminderSettingsRepository settingsRepository;

	@Mock
	private BillRepository billRepository;

	@Mock
	private RentalContractRepository contractRepository;

	@Mock
	private TelegramApiClient telegramApiClient;

	private final ReminderSettings settings = ReminderSettings.createDefault();

	@BeforeEach
	void setUp() {
		when(settingsRepository.findSingleton()).thenReturn(settings);
		org.mockito.Mockito.lenient().when(reminderRepository.save(any()))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	private ReminderDispatchService serviceAt(LocalDate date) {
		return new ReminderDispatchService(
			reminderRepository, settingsRepository, billRepository, contractRepository,
			new ReminderMessageBuilder(), telegramApiClient,
			new TelegramProperties("token", "chat-id", "https://api.telegram.org"),
			Clock.fixed(date.atTime(LocalTime.NOON).atZone(VIETNAM).toInstant(), VIETNAM));
	}

	private void runAt(LocalDate date) {
		serviceAt(date).runDailyJob();
	}

	/** Hóa đơn có ngày đến hạn đúng bằng dueDate mong muốn (paymentDueDay = dayOfMonth). */
	private Bill unpaidBill(LocalDate dueDate) {
		RentalContract contract = RentalContract.createActive(new CreateRentalContractCommand(
			LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2), dueDate.getDayOfMonth(),
			4_400_000L, 3_800L, 200_000L, 100_000L));
		return Bill.createFrom(contract, 2_000L, 1_000L, "%d-%02d".formatted(dueDate.getYear(), dueDate.getMonthValue()));
	}

	private RentalContract activeContract() {
		return RentalContract.createActive(new CreateRentalContractCommand(
			LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2), 4,
			4_400_000L, 3_800L, 200_000L, 100_000L));
	}

	private void stubExistingReminder(Optional<Reminder> existing) {
		when(reminderRepository.findByReminderTypeAndReferenceIdAndTargetDateAndChannel(
			any(), any(), any(), any())).thenReturn(existing);
	}

	@Test
	void sendsUpcomingReminderSevenDaysBeforeDueDate() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		runAt(dueDate.minusDays(7));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(telegramApiClient).sendMessage(eq("chat-id"), messageCaptor.capture());
		assertThat(messageCaptor.getValue()).contains("Nhắc thanh toán").contains(bill.getPeriod());

		ArgumentCaptor<Reminder> reminderCaptor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(reminderCaptor.capture());
		assertThat(reminderCaptor.getValue().getStatus()).isEqualTo(ReminderStatus.SENT);
		assertThat(reminderCaptor.getValue().getTargetDate()).isEqualTo(dueDate.minusDays(7));
	}

	@Test
	void doesNotSendUpcomingReminderOnOtherDays() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		runAt(dueDate.minusDays(9));

		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
	}

	@Test
	void marksUnpaidBillsOverdueAfterDueDate() {
		LocalDate dueDate = LocalDate.of(2026, 9, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		runAt(dueDate.plusDays(2));

		assertThat(bill.getStatus()).isEqualTo(BillStatus.OVERDUE);
	}

	@Test
	void sendsOverdueReminderOnConfiguredOverdueDay() {
		LocalDate dueDate = LocalDate.of(2026, 9, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.OVERDUE);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		runAt(dueDate.plusDays(2));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(telegramApiClient).sendMessage(eq("chat-id"), messageCaptor.capture());
		assertThat(messageCaptor.getValue()).contains("Quá hạn").contains("ngày trễ thứ 4");
	}

	@Test
	void terminatesContractOnFourthOverdueDayAndSendsSingleNotification() {
		LocalDate dueDate = LocalDate.of(2026, 9, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.OVERDUE);
		RentalContract contract = activeContract();
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));
		when(contractRepository.findById(bill.getContractId())).thenReturn(Optional.of(contract));

		runAt(dueDate.plusDays(4));

		assertThat(contract.getStatus()).isEqualTo(RentalContractStatus.TERMINATED_FOR_NON_PAYMENT);
		verify(contractRepository).save(contract);
		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(telegramApiClient).sendMessage(eq("chat-id"), messageCaptor.capture());
		assertThat(messageCaptor.getValue()).contains("đã bị hủy");
	}

	@Test
	void skipsTerminationAndNotificationForAlreadyTerminatedContract() {
		LocalDate dueDate = LocalDate.of(2026, 9, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.OVERDUE);
		RentalContract contract = activeContract();
		contract.setStatus(RentalContractStatus.TERMINATED_FOR_NON_PAYMENT);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));
		when(contractRepository.findById(bill.getContractId())).thenReturn(Optional.of(contract));

		runAt(dueDate.plusDays(4));

		verify(contractRepository, never()).save(any());
		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
	}

	@Test
	void neverRemindsPaidBills() {
		when(billRepository.findByStatusIn(any())).thenReturn(List.of());

		runAt(LocalDate.of(2026, 10, 5));

		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
	}

	@Test
	void doesNotSendWhenBillRemindersDisabled() {
		ReminderSettings disabled = ReminderSettings.createDefault();
		disabled.apply(false, disabled.billReminderDaysBeforeList(),
			disabled.isOverdueRemindersEnabled(), disabled.overdueReminderDaysList(),
			disabled.isContractRemindersEnabled(), disabled.contractReminderDaysBeforeList());
		when(settingsRepository.findSingleton()).thenReturn(disabled);
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		runAt(dueDate.minusDays(7));

		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
	}

	@Test
	void recordsFailedReminderWhenTelegramRejects() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));
		org.mockito.Mockito.doThrow(new TelegramSendException("Telegram từ chối yêu cầu"))
			.when(telegramApiClient).sendMessage(anyString(), anyString());

		runAt(dueDate.minusDays(7));

		ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ReminderStatus.FAILED);
		assertThat(captor.getValue().getErrorCode()).isEqualTo("TELEGRAM_SEND_FAILED");
		assertThat(captor.getValue().canRetry()).isTrue();
	}

	@Test
	void retriesFailedReminderOnNextRun() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		Reminder failed = Reminder.create(ReminderType.BILL_UPCOMING, bill.getId(),
			dueDate.minusDays(7), ReminderChannel.TELEGRAM);
		failed.markAttempt();
		failed.markFailed("TELEGRAM_SEND_FAILED");
		stubExistingReminder(Optional.of(failed));

		runAt(dueDate.minusDays(7));

		ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(captor.capture());
		assertThat(captor.getValue().getAttemptCount()).isEqualTo(2);
		assertThat(captor.getValue().getStatus()).isEqualTo(ReminderStatus.SENT);
	}

	@Test
	void stopsRetryingAfterMaxAttempts() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		Reminder exhausted = Reminder.create(ReminderType.BILL_UPCOMING, bill.getId(),
			dueDate.minusDays(7), ReminderChannel.TELEGRAM);
		exhausted.markAttempt();
		exhausted.markAttempt();
		exhausted.markAttempt();
		exhausted.markFailed("TELEGRAM_SEND_FAILED");
		stubExistingReminder(Optional.of(exhausted));

		runAt(dueDate.minusDays(7));

		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
		verify(reminderRepository, never()).save(any());
	}

	@Test
	void recordsFailedWhenTelegramNotConfigured() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));
		ReminderDispatchService unconfigured = new ReminderDispatchService(
			reminderRepository, settingsRepository, billRepository, contractRepository,
			new ReminderMessageBuilder(), telegramApiClient,
			new TelegramProperties("", "", "https://api.telegram.org"),
			Clock.fixed(dueDate.minusDays(7).atTime(LocalTime.NOON).atZone(VIETNAM).toInstant(), VIETNAM));

		unconfigured.runDailyJob();

		ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
		verify(reminderRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(ReminderStatus.FAILED);
		assertThat(captor.getValue().getErrorCode()).isEqualTo("TELEGRAM_NOT_CONFIGURED");
		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
	}

	@Test
	void sendsContractExpiringReminderOnConfiguredDays() {
		LocalDate endDate = LocalDate.of(2027, 2, 2);
		RentalContract contract = activeContract();
		when(billRepository.findByStatusIn(any())).thenReturn(List.of());
		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE)).thenReturn(Optional.of(contract));

		runAt(endDate.minusDays(60));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(telegramApiClient).sendMessage(eq("chat-id"), messageCaptor.capture());
		assertThat(messageCaptor.getValue()).contains("sắp hết hạn");
	}

	@Test
	void dedupesSameReminderWhenJobRunsTwiceInADay() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		Reminder sent = Reminder.create(ReminderType.BILL_UPCOMING, bill.getId(),
			dueDate.minusDays(7), ReminderChannel.TELEGRAM);
		sent.markAttempt();
		sent.markSent(Instant.now());
		stubExistingReminder(Optional.of(sent));

		runAt(dueDate.minusDays(7));

		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
		verify(reminderRepository, never()).save(any());
	}
}
