package com.khoa.roommanagement.reminders.service;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.entity.BillStatus;
import com.khoa.roommanagement.billing.bills.repository.BillRepository;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderChannel;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderType;
import com.khoa.roommanagement.reminders.reminder.repository.ReminderRepository;
import com.khoa.roommanagement.reminders.settings.entity.ReminderSettings;
import com.khoa.roommanagement.reminders.settings.repository.ReminderSettingsRepository;
import com.khoa.roommanagement.reminders.telegram.TelegramProperties;
import com.khoa.roommanagement.reminders.telegram.TelegramSendException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job hằng ngày: tính các nhắc đến hạn theo cấu hình, chống gửi trùng theo
 * khóa (loại, đối tượng, ngày dự kiến gửi, kênh) và hủy hợp đồng ở ngày trễ
 * thứ tư. Mọi trạng thái đều nằm trong database nên restart không gửi trùng.
 */
@Service
public class ReminderDispatchService {

	private static final Logger log = LoggerFactory.getLogger(ReminderDispatchService.class);

	private static final int OVERDUE_TERMINATION_DAY = 4;

	private final ReminderRepository reminderRepository;
	private final ReminderSettingsRepository settingsRepository;
	private final BillRepository billRepository;
	private final RentalContractRepository contractRepository;
	private final ReminderMessageBuilder messageBuilder;
	private final com.khoa.roommanagement.reminders.telegram.TelegramApiClient telegramApiClient;
	private final TelegramProperties telegramProperties;
	private final Clock clock;

	public ReminderDispatchService(
		ReminderRepository reminderRepository,
		ReminderSettingsRepository settingsRepository,
		BillRepository billRepository,
		RentalContractRepository contractRepository,
		ReminderMessageBuilder messageBuilder,
		com.khoa.roommanagement.reminders.telegram.TelegramApiClient telegramApiClient,
		TelegramProperties telegramProperties,
		Clock clock
	) {
		this.reminderRepository = reminderRepository;
		this.settingsRepository = settingsRepository;
		this.billRepository = billRepository;
		this.contractRepository = contractRepository;
		this.messageBuilder = messageBuilder;
		this.telegramApiClient = telegramApiClient;
		this.telegramProperties = telegramProperties;
		this.clock = clock;
	}

	@Transactional
	public void runDailyJob() {
		ReminderSettings settings = settingsRepository.findSingleton();
		LocalDate today = LocalDate.now(clock);

		markUnpaidBillsOverdue(today);
		List<Bill> unpaidBills = billRepository.findByStatusIn(List.of(BillStatus.PENDING, BillStatus.OVERDUE));

		for (Bill bill : unpaidBills) {
			long daysUntilDue = ChronoUnit.DAYS.between(today, bill.getDueDate());
			long daysPastDue = ChronoUnit.DAYS.between(bill.getDueDate(), today);

			if (settings.isBillRemindersEnabled() && daysUntilDue > 0) {
				for (Integer daysBefore : settings.billReminderDaysBeforeList()) {
					LocalDate expectedSendDate = bill.getDueDate().minusDays(daysBefore);
					if (today.equals(expectedSendDate)) {
						dispatch(ReminderType.BILL_UPCOMING, bill.getId(), expectedSendDate,
							messageBuilder.billUpcoming(bill));
					}
				}
			}

			if (settings.isOverdueRemindersEnabled() && daysPastDue >= 1 && daysPastDue < OVERDUE_TERMINATION_DAY) {
				for (Integer daysAfter : settings.overdueReminderDaysList()) {
					LocalDate expectedSendDate = bill.getDueDate().plusDays(daysAfter);
					if (today.equals(expectedSendDate)) {
						dispatch(ReminderType.BILL_OVERDUE, bill.getId(), expectedSendDate,
							messageBuilder.billOverdue(bill, daysPastDue));
					}
				}
			}

			// Ngày trễ thứ tư (hoặc muộn hơn nếu job nghỉ): hủy hợp đồng và gửi
			// đúng một thông báo xác nhận. Hành động nghiệp vụ, không phụ thuộc
			// bật/tắt nhắc.
			if (daysPastDue >= OVERDUE_TERMINATION_DAY) {
				terminateContractAndNotify(bill, today);
			}
		}

		if (settings.isContractRemindersEnabled()) {
			notifyExpiringContract(today, settings);
		}
	}

	private void markUnpaidBillsOverdue(LocalDate today) {
		for (Bill bill : billRepository.findByStatusIn(List.of(BillStatus.PENDING))) {
			if (bill.getDueDate().isBefore(today)) {
				bill.setStatus(BillStatus.OVERDUE);
				billRepository.save(bill);
			}
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

		dispatch(ReminderType.CONTRACT_TERMINATED, contract.getId(),
			bill.getDueDate().plusDays(OVERDUE_TERMINATION_DAY),
			messageBuilder.contractTerminated(bill));
	}

	private void notifyExpiringContract(LocalDate today, ReminderSettings settings) {
		Optional<RentalContract> activeContract = contractRepository.findByStatus(RentalContractStatus.ACTIVE);
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
				dispatch(ReminderType.CONTRACT_EXPIRING, contract.getId(), expectedSendDate,
					messageBuilder.contractExpiring(contract, daysUntilEnd));
			}
		}
	}

	/**
	 * Gửi một nhắc với chống trùng: SENT cho cùng khóa → bỏ qua; FAILED còn
	 * lượt retry → thử lại; không có bản ghi → tạo mới.
	 */
	private void dispatch(ReminderType type, java.util.UUID referenceId, LocalDate targetDate, String message) {
		Optional<Reminder> existing = reminderRepository
			.findByReminderTypeAndReferenceIdAndTargetDateAndChannel(type, referenceId, targetDate, ReminderChannel.TELEGRAM);
		if (existing.isPresent() && !existing.get().canRetry()) {
			return;
		}

		Reminder reminder = existing.orElseGet(() -> Reminder.create(type, referenceId, targetDate, ReminderChannel.TELEGRAM));
		reminder.markAttempt();

		if (!telegramProperties.isConfigured()) {
			reminder.markFailed("TELEGRAM_NOT_CONFIGURED");
			reminderRepository.save(reminder);
			return;
		}

		try {
			telegramApiClient.sendMessage(telegramProperties.chatId(), message);
			reminder.markSent(Instant.now(clock));
		} catch (TelegramSendException exception) {
			reminder.markFailed(exception.getSafeErrorCode());
		}
		reminderRepository.save(reminder);
	}
}
