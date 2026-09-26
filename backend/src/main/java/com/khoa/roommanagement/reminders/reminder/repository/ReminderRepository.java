package com.khoa.roommanagement.reminders.reminder.repository;

import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderChannel;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderStatus;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

	Optional<Reminder> findByReminderTypeAndReferenceIdAndTargetDateAndChannel(
		ReminderType reminderType,
		UUID referenceId,
		LocalDate targetDate,
		ReminderChannel channel
	);

	List<Reminder> findByStatusInAndTargetDateGreaterThanEqual(List<ReminderStatus> statuses, LocalDate targetDate);

	Page<Reminder> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
