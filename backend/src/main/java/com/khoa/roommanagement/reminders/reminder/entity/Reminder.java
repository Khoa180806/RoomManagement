package com.khoa.roommanagement.reminders.reminder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reminders")
public class Reminder {

	public static final int MAX_ATTEMPTS = 3;

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "reminder_type", nullable = false, length = 32)
	private ReminderType reminderType;

	@Column(name = "reference_id", nullable = false)
	private UUID referenceId;

	@Column(name = "target_date", nullable = false)
	private LocalDate targetDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private ReminderChannel channel;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private ReminderStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "sent_at")
	private Instant sentAt;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Reminder() {
	}

	public static Reminder create(ReminderType reminderType, UUID referenceId, LocalDate targetDate,
			ReminderChannel channel) {
		Reminder reminder = new Reminder();
		reminder.id = UUID.randomUUID();
		reminder.reminderType = reminderType;
		reminder.referenceId = referenceId;
		reminder.targetDate = targetDate;
		reminder.channel = channel;
		reminder.status = ReminderStatus.FAILED;
		reminder.attemptCount = 0;
		reminder.createdAt = Instant.now();
		return reminder;
	}

	public void markAttempt() {
		this.attemptCount += 1;
	}

	public void markSent(Instant sentAt) {
		this.status = ReminderStatus.SENT;
		this.sentAt = sentAt;
		this.errorCode = null;
	}

	public void markFailed(String safeErrorCode) {
		this.status = ReminderStatus.FAILED;
		this.errorCode = safeErrorCode;
	}

	public boolean canRetry() {
		return status == ReminderStatus.FAILED && attemptCount < MAX_ATTEMPTS;
	}

	public UUID getId() {
		return id;
	}

	public ReminderType getReminderType() {
		return reminderType;
	}

	public UUID getReferenceId() {
		return referenceId;
	}

	public LocalDate getTargetDate() {
		return targetDate;
	}

	public ReminderChannel getChannel() {
		return channel;
	}

	public ReminderStatus getStatus() {
		return status;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
