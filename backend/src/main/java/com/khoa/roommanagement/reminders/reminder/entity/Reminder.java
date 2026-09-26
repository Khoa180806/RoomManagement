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

	/** Số lần thử tối đa cho một reminder. */
	public static final int MAX_ATTEMPTS = 3;

	/** Khoảng cách (ngày) giữa hai lần retry liên tiếp. */
	public static final long BACKOFF_DAYS = 2;

	/** Chỉ retry các reminder trong cửa sổ này kể từ ngày dự kiến gửi. */
	public static final long RETRY_WINDOW_DAYS = 7;

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

	/** Snapshot nội dung tin nhắn để retry không phải dựng lại từ dữ liệu gốc. */
	@Column(nullable = false, columnDefinition = "TEXT")
	private String message;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Reminder() {
	}

	public static Reminder create(ReminderType reminderType, UUID referenceId, LocalDate targetDate,
			ReminderChannel channel, String message) {
		Reminder reminder = new Reminder();
		reminder.id = UUID.randomUUID();
		reminder.reminderType = reminderType;
		reminder.referenceId = referenceId;
		reminder.targetDate = targetDate;
		reminder.channel = channel;
		reminder.status = ReminderStatus.PENDING;
		reminder.attemptCount = 0;
		reminder.message = message;
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

	/**
	 * Chưa hoàn thành và còn lượt thử. Backoff: sau lần thử thứ k, lần kế tiếp
	 * chỉ từ ngày targetDate + (k-1) × BACKOFF_DAYS.
	 */
	public boolean canRetry(LocalDate today) {
		return status != ReminderStatus.SENT
			&& attemptCount < MAX_ATTEMPTS
			&& !today.isBefore(nextRetryDate());
	}

	public LocalDate nextRetryDate() {
		// Chưa thử lần nào → gửi ngay vào ngày dự kiến; sau lần thử thứ k,
		// lần kế tiếp lùi BACKOFF_DAYS so với lần trước đó.
		long backoffDays = Math.max(0, attemptCount - 1) * BACKOFF_DAYS;
		return targetDate.plusDays(backoffDays);
	}

	public boolean isWithinRetryWindow(LocalDate today) {
		return !today.isAfter(targetDate.plusDays(RETRY_WINDOW_DAYS));
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

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
