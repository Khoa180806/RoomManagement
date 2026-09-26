package com.khoa.roommanagement.reminders.reminder.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khoa.roommanagement.RoomManagementApiApplication;
import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderChannel;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderType;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test ràng buộc unique chống gửi trùng theo khóa
 * (loại nhắc, đối tượng, ngày dự kiến gửi, kênh) ở mức database.
 */
@SpringBootTest(classes = RoomManagementApiApplication.class)
@Transactional
class ReminderRepositoryIntegrationTest {

	@Autowired
	private ReminderRepository reminderRepository;

	@Test
	void rejectsDuplicateReminderForSameKey() {
		UUID billId = UUID.randomUUID();
		LocalDate targetDate = LocalDate.of(2026, 9, 6);

		Reminder first = Reminder.create(ReminderType.BILL_OVERDUE, billId, targetDate, ReminderChannel.TELEGRAM);
		reminderRepository.saveAndFlush(first);

		Reminder duplicate = Reminder.create(ReminderType.BILL_OVERDUE, billId, targetDate, ReminderChannel.TELEGRAM);
		// Sau khi session hỏng do violation không truy vấn tiếp được trong cùng
		// transaction; việc chặn trùng đã được chứng minh bởi exception dưới đây.
		assertThatThrownBy(() -> reminderRepository.saveAndFlush(duplicate))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void allowsReminderForDifferentTargetDate() {
		UUID billId = UUID.randomUUID();

		reminderRepository.saveAndFlush(Reminder.create(
			ReminderType.BILL_OVERDUE, billId, LocalDate.of(2026, 9, 6), ReminderChannel.TELEGRAM));
		reminderRepository.saveAndFlush(Reminder.create(
			ReminderType.BILL_OVERDUE, billId, LocalDate.of(2026, 9, 7), ReminderChannel.TELEGRAM));

		assertThat(reminderRepository.count()).isEqualTo(2);
	}
}
