package com.khoa.roommanagement.reminders.service;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.repository.BillRepository;
import com.khoa.roommanagement.reminders.reminder.dto.ReminderResponse;
import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderType;
import com.khoa.roommanagement.reminders.reminder.repository.ReminderRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderHistoryService {

	private static final int MAX_PAGE_SIZE = 100;

	private final ReminderRepository reminderRepository;
	private final BillRepository billRepository;

	public ReminderHistoryService(ReminderRepository reminderRepository, BillRepository billRepository) {
		this.reminderRepository = reminderRepository;
		this.billRepository = billRepository;
	}

	@Transactional(readOnly = true)
	public Page<ReminderResponse> getReminderHistory(int page, Integer pageSize) {
		int requestedSize = pageSize != null ? pageSize : 20;
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(requestedSize, 1), MAX_PAGE_SIZE));
		return reminderRepository.findAllByOrderByCreatedAtDesc(pageable)
			.map(this::toResponse);
	}

	private ReminderResponse toResponse(Reminder reminder) {
		if (reminder.getReminderType() == ReminderType.BILL_UPCOMING
			|| reminder.getReminderType() == ReminderType.BILL_OVERDUE) {
			Optional<Bill> bill = billRepository.findById(reminder.getReferenceId());
			return bill
				.map(value -> ReminderResponse.from(reminder, value.getPeriod(), value.getTotalAmount()))
				.orElseGet(() -> ReminderResponse.from(reminder, null, null));
		}
		return ReminderResponse.from(reminder, null, null);
	}
}
