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
import com.khoa.roommanagement.billing.bills.service.BillService;
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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
	private BillService billService;

	@Mock
	private ReminderSender reminderSender;

	private final ReminderSettings settings = ReminderSettings.createDefault();

	@BeforeEach
	void setUp() {
		when(settingsRepository.findSingleton()).thenReturn(settings);
	}

	private ReminderDispatchService serviceAt(LocalDate date) {
		return new ReminderDispatchService(
			reminderRepository, settingsRepository, billRepository, contractRepository,
			billService, new ReminderMessageBuilder(), reminderSender,
			Clock.fixed(date.atTime(LocalTime.NOON).atZone(VIETNAM).toInstant(), VIETNAM));
	}

	private void runAt(LocalDate date) {
		serviceAt(date).runDailyJob();
	}

	private Bill unpaidBill(LocalDate dueDate) {
		RentalContract contract = RentalContract.createActive(new CreateRentalContractCommand(
			LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2), dueDate.getDayOfMonth(),
			4_400_000L, 3_800L, 200_000L, 100_000L));
		return Bill.createFrom(contract, 2_000L, 1_000L,
			"%d-%02d".formatted(dueDate.getYear(), dueDate.getMonthValue()));
	}

	private RentalContract activeContract() {
		return RentalContract.createActive(new CreateRentalContractCommand(
			LocalDate.of(2026, 2, 2), LocalDate.of(2027, 2, 2), 4,
			4_400_000L, 3_800L, 200_000L, 100_000L));
	}

	@Test
	void asksBillingToMarkOverdueBills() {
		runAt(LocalDate.of(2026, 9, 7));

		verify(billService).markOverdueBills(LocalDate.of(2026, 9, 7));
	}

	@Test
	void schedulesUpcomingReminderSevenDaysBeforeDueDate() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		runAt(dueDate.minusDays(7));

		verify(reminderSender).dispatchScheduled(eq(ReminderType.BILL_UPCOMING), eq(bill.getId()),
			eq(dueDate.minusDays(7)), anyString());
	}

	@Test
	void doesNotScheduleUpcomingReminderOnOtherDays() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		runAt(dueDate.minusDays(9));

		verify(reminderSender, never()).dispatchScheduled(any(), any(), any(), anyString());
	}

	@Test
	void schedulesOverdueReminderOnConfiguredOverdueDayWithWarning() {
		LocalDate dueDate = LocalDate.of(2026, 9, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.OVERDUE);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		runAt(dueDate.plusDays(2));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(reminderSender).dispatchScheduled(eq(ReminderType.BILL_OVERDUE), eq(bill.getId()),
			eq(dueDate.plusDays(2)), messageCaptor.capture());
		assertThat(messageCaptor.getValue()).contains("Quá hạn").contains("ngày trễ thứ 4");
	}

	@Test
	void terminatesContractOnFourthOverdueDayAndSchedulesSingleNotification() {
		LocalDate dueDate = LocalDate.of(2026, 9, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.OVERDUE);
		RentalContract contract = activeContract();
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));
		when(contractRepository.findById(bill.getContractId())).thenReturn(Optional.of(contract));

		runAt(dueDate.plusDays(4));

		assertThat(contract.getStatus()).isEqualTo(RentalContractStatus.TERMINATED_FOR_NON_PAYMENT);
		verify(contractRepository).save(contract);
		verify(reminderSender).dispatchScheduled(eq(ReminderType.CONTRACT_TERMINATED), eq(contract.getId()),
			eq(dueDate.plusDays(4)), anyString());
	}

	@Test
	void doesNotTerminateAlreadyTerminatedContract() {
		LocalDate dueDate = LocalDate.of(2026, 9, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.OVERDUE);
		RentalContract contract = activeContract();
		contract.setStatus(RentalContractStatus.TERMINATED_FOR_NON_PAYMENT);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));
		when(contractRepository.findById(bill.getContractId())).thenReturn(Optional.of(contract));

		runAt(dueDate.plusDays(4));

		verify(contractRepository, never()).save(any());
		verify(reminderSender, never()).dispatchScheduled(any(), any(), any(), anyString());
	}

	@Test
	void neverRemindsPaidBills() {
		when(billRepository.findByStatusIn(any())).thenReturn(List.of());

		runAt(LocalDate.of(2026, 10, 5));

		verify(reminderSender, never()).dispatchScheduled(any(), any(), any(), anyString());
	}

	@Test
	void doesNotScheduleWhenBillRemindersDisabled() {
		ReminderSettings disabled = ReminderSettings.createDefault();
		disabled.apply(new ReminderSettings.ReminderDayRule(false, List.of(1, 3, 7)),
			new ReminderSettings.ReminderDayRule(true, List.of(1, 2, 3)),
			new ReminderSettings.ReminderDayRule(true, List.of(30, 60)));
		when(settingsRepository.findSingleton()).thenReturn(disabled);
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill bill = unpaidBill(dueDate);
		bill.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(bill));

		runAt(dueDate.minusDays(7));

		verify(reminderSender, never()).dispatchScheduled(any(), any(), any(), anyString());
	}

	@Test
	void schedulesContractExpiringReminderOnConfiguredDays() {
		LocalDate endDate = LocalDate.of(2027, 2, 2);
		RentalContract contract = activeContract();
		when(billRepository.findByStatusIn(any())).thenReturn(List.of());
		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE)).thenReturn(Optional.of(contract));

		runAt(endDate.minusDays(60));

		verify(reminderSender).dispatchScheduled(eq(ReminderType.CONTRACT_EXPIRING), eq(contract.getId()),
			eq(endDate.minusDays(60)), anyString());
	}

	@Test
	void retriesDueFailedRemindersOutsideTheirTargetDate() {
		LocalDate today = LocalDate.of(2026, 9, 29);
		Reminder failed = Reminder.create(ReminderType.BILL_UPCOMING, UUID.randomUUID(),
			LocalDate.of(2026, 9, 26), ReminderChannel.TELEGRAM, "msg");
		failed.markAttempt();
		failed.markFailed("TELEGRAM_SEND_FAILED");
		when(reminderRepository.findByStatusInAndTargetDateGreaterThanEqual(
			eq(List.of(ReminderStatus.PENDING, ReminderStatus.FAILED)), eq(today.minusDays(Reminder.RETRY_WINDOW_DAYS))))
			.thenReturn(List.of(failed));

		runAt(today);

		verify(reminderSender).dispatchRetry(failed.getId());
	}

	@Test
	void skipsRetryForRemindersTargetedTodayAndBeyondBackoff() {
		LocalDate today = LocalDate.of(2026, 9, 26);
		// Nhắc đúng hôm nay đã nằm ở đường scheduled; retry pass phải bỏ qua.
		Reminder targetedToday = Reminder.create(ReminderType.BILL_UPCOMING, UUID.randomUUID(),
			today, ReminderChannel.TELEGRAM, "msg");
		targetedToday.markAttempt();
		targetedToday.markFailed("TELEGRAM_SEND_FAILED");
		// Backoff chưa tới: attemptCount=2 cần chờ targetDate + 2 ngày.
		Reminder backoffPending = Reminder.create(ReminderType.BILL_OVERDUE, UUID.randomUUID(),
			today.minusDays(1), ReminderChannel.TELEGRAM, "msg");
		backoffPending.markAttempt();
		backoffPending.markFailed("TELEGRAM_SEND_FAILED");
		backoffPending.markAttempt();
		backoffPending.markFailed("TELEGRAM_SEND_FAILED");
		when(reminderRepository.findByStatusInAndTargetDateGreaterThanEqual(any(), any()))
			.thenReturn(List.of(targetedToday, backoffPending));

		runAt(today);

		verify(reminderSender, never()).dispatchRetry(any());
	}

	@Test
	void continuesWithRemainingBillsWhenOneBillFails() {
		LocalDate dueDate = LocalDate.of(2026, 10, 5);
		Bill broken = unpaidBill(dueDate);
		broken.setStatus(BillStatus.PENDING);
		Bill healthy = unpaidBill(dueDate);
		healthy.setStatus(BillStatus.PENDING);
		when(billRepository.findByStatusIn(any())).thenReturn(List.of(broken, healthy));
		// reminderSender ném exception bất ngờ (ví dụ DB lỗi) cho bill đầu.
		org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
			.when(reminderSender).dispatchScheduled(any(), eq(broken.getId()), any(), anyString());

		runAt(dueDate.minusDays(7));

		verify(reminderSender).dispatchScheduled(eq(ReminderType.BILL_UPCOMING), eq(healthy.getId()),
			eq(dueDate.minusDays(7)), anyString());
	}
}
