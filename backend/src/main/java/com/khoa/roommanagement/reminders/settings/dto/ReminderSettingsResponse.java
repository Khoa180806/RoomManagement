package com.khoa.roommanagement.reminders.settings.dto;

import java.time.Instant;
import java.util.List;

public record ReminderSettingsResponse(
    boolean billRemindersEnabled,
    List<Integer> billReminderDaysBefore,
    boolean overdueRemindersEnabled,
    List<Integer> overdueReminderDays,
    boolean contractRemindersEnabled,
    List<Integer> contractReminderDaysBefore,
    Instant updatedAt
) {
}
