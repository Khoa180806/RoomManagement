package com.khoa.roommanagement.reminders.reminder.dto;

import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReminderResponse(
    UUID id,
    String reminderType,
    UUID referenceId,
    LocalDate targetDate,
    String channel,
    String status,
    int attemptCount,
    Instant sentAt,
    String errorCode,
    Instant createdAt,
    String billPeriod,
    Long billTotalAmount
) {
    public static ReminderResponse from(Reminder reminder, String billPeriod, Long billTotalAmount) {
        return new ReminderResponse(
            reminder.getId(),
            reminder.getReminderType().name(),
            reminder.getReferenceId(),
            reminder.getTargetDate(),
            reminder.getChannel().name(),
            reminder.getStatus().name(),
            reminder.getAttemptCount(),
            reminder.getSentAt(),
            reminder.getErrorCode(),
            reminder.getCreatedAt(),
            billPeriod,
            billTotalAmount
        );
    }
}
