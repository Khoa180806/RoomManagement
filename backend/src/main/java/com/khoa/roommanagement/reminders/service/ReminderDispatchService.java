package com.khoa.roommanagement.reminders.service;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.entity.BillStatus;
import com.khoa.roommanagement.billing.bills.repository.BillRepository;
import com.khoa.roommanagement.billing.bills.service.BillService;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderStatus;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderType;
import com.khoa.roommanagement.reminders.reminder.repository.ReminderRepository;
import com.khoa.roommanagement.reminders.settings.entity.ReminderSettings;
import com.khoa.roommanagement.reminders.settings.repository.ReminderSettingsRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrator job hằng ngày: tính các nhắc đến hạn theo cấu hình, hủy hợp
 * đồng ở ngày trễ thứ tư và lên lịch retry. Không giữ transaction xuyên suốt
 * — mỗi lần gửi nằm trong transaction riêng của {@link ReminderSender}, và
 * lỗi một bill không làm bỏ qua phần còn lại của ngày.
 */
@Service
public class ReminderDispatchService {

	private static final Logger log = LoggerFactory.getLogger(ReminderDispatchService.class);

	private static final int OVERDUE_TERMINATION_DAY = 4;

	private final ReminderRepository reminderRepository;
	private final ReminderSettingsRepository settingsRepository;
	private final BillRepository billRepository;
	private final RentalContractRepository contractRepository;
	private final BillService billService;
	private final ReminderMessageBuilder messageBuilder;
	private final ReminderSender reminderSender;
	private final Clock clock;

	public ReminderDispatchService(
		ReminderRepository reminderRepository,
		ReminderSettingsRepository settingsRepository,
		BillRepository billRepository,
		RentalContractRepository contractRepository,
		BillService billService,
		ReminderMessageBuilder messageBuilder,
		ReminderSender reminderSender,
		Clock clock
	) {
		this.reminderRepository = reminderRepository;
		this.settingsRepository = settingsRepository;
		this.billRepository = billRepository;
		this.contractRepository = contractRepository;
		this.billService = billService;
		this.messageBuilder = messageBuilder;
		this.reminderSender = reminderSender;
		this.clock = clock;
	}

	public void runDailyJob() {
		ReminderSettings settings = settingsRepository.findSingleton();
		LocalDate today = LocalDate.now(clock);

		try {
			// Capability thuộc billing; scheduler chỉ kích hoạt hàng ngày.
			billService.markOverdueBills(today);
		} catch (RuntimeException exception) {
			log.warn("Failed to mark overdue bills; skipping this step", exception);
		}

		try {
			for (Bill bill : billRepository.findByStatusIn(List.of(BillStatus.PENDING, BillStatus.OVERDUE))) {
				try {
					processBill(bill, today, settings);
				} catch (RuntimeException exception) {
					log.warn("Failed to process bill {}; continuing with the rest", bill.getId(), exception);
				}
			}
		} catch (RuntimeException exception) {
			log.warn("Failed to load unpaid bills; skipping bill reminders today", exception);
		}

		try {
			notifyExpiringContract(today, settings);
		} catch (RuntimeException exception) {
			log.warn("Failed to check contract expiration; skipping this step", exception);
		}

		try {
			retryDueReminders(today);
		} catch (RuntimeException exception) {
			log.warn("Failed to run reminder retry pass", exception);
		}
	}

	private void processBill(Bill bill, LocalDate today, ReminderSettings settings) {
		long daysUntilDue = ChronoUnit.DAYS.between(today, bill.getDueDate());
		long daysPastDue = ChronoUnit.DAYS.between(bill.getDueDate(), today);

		if (settings.isBillRemindersEnabled() && daysUntilDue > 0) {
			for (Integer daysBefore : settings.billReminderDaysBeforeList()) {
				LocalDate expectedSendDate = bill.getDueDate().minusDays(daysBefore);
				if (today.equals(expectedSendDate)) {
					reminderSender.dispatchScheduled(ReminderType.BILL_UPCOMING, bill.getId(), expectedSendDate,
						messageBuilder.billUpcoming(bill));
				}
			}
		}

		if (settings.isOverdueRemindersEnabled() && daysPastDue >= 1 && daysPastDue < OVERDUE_TERMINATION_DAY) {
			for (Integer daysAfter : settings.overdueReminderDaysList()) {
				LocalDate expectedSendDate = bill.getDueDate().plusDays(daysAfter);
				if (today.equals(expectedSendDate)) {
					reminderSender.dispatchScheduled(ReminderType.BILL_OVERDUE, bill.getId(), expectedSendDate,
						messageBuilder.billOverdue(bill, daysPastDue));
				}
			}
		}

		// Ngày trễ thứ tư (hoặc muộn hơn nếu job nghỉ): hủy hợp đồng; thông báo
		// xác nhận gửi đúng một lần qua dedupe và được retry nếu Telegram lỗi.
		if (daysPastDue >= OVERDUE_TERMINATION_DAY) {
			terminateContractAndNotify(bill, today);
		}
	}

	private void terminateContractAndNotify(Bill bill, LocalDate today) {
		RentalContract contract = contractRepository.findById(bill.getContractId())
			.orElse(null);
		if (contract == null || contract.getStatus() != RentalContractStatus.ACTIVE) {
			return;
		}

		contract.setStatus(RentalContractStatus.TERMINATED_FOR_NON_PAYMENT);
		contractRepository.save(contract);
		log.warn("Contract {} terminated for non-payment (bill period {})",
			contract.getId(), bill.getPeriod());

		reminderSender.dispatchScheduled(ReminderType.CONTRACT_TERMINATED, contract.getId(),
			bill.getDueDate().plusDays(OVERDUE_TERMINATION_DAY),
			messageBuilder.contractTerminated(bill));
	}

	private void notifyExpiringContract(LocalDate today, ReminderSettings settings) {
		var activeContract = contractRepository.findByStatus(RentalContractStatus.ACTIVE);
		if (activeContract.isEmpty()) {
			return;
		}
		RentalContract contract = activeContract.get();
		long daysUntilEnd = ChronoUnit.DAYS.between(today, contract.getEndDate());
		if (daysUntilEnd <= 0) {
			return;
		}
		for (Integer daysBefore : settings.contractReminderDaysBeforeList()) {
			LocalDate expectedSendDate = contract.getEndDate().minusDays(daysBefore);
			if (today.equals(expectedSendDate)) {
				reminderSender.dispatchScheduled(ReminderType.CONTRACT_EXPIRING, contract.getId(), expectedSendDate,
					messageBuilder.contractExpiring(contract, daysUntilEnd));
			}
		}
	}

	/** Retry các reminder FAILED/PENDING còn trong cửa sổ, có tính backoff. */
	private void retryDueReminders(LocalDate today) {
		List<Reminder> candidates = reminderRepository.findByStatusInAndTargetDateGreaterThanEqual(
			List.of(ReminderStatus.PENDING, ReminderStatus.FAILED),
			today.minusDays(Reminder.RETRY_WINDOW_DAYS));
		for (Reminder reminder : candidates) {
			// Ngày dự kiến gửi hôm nay đã được xử lý ở đường scheduled phía trên.
			if (today.equals(reminder.getTargetDate())) {
				continue;
			}
			if (reminder.canRetry(today)) {
				reminderSender.dispatchRetry(reminder.getId());
			}
		}
	}
}
