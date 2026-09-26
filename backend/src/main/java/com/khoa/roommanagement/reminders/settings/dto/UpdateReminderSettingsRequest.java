package com.khoa.roommanagement.reminders.settings.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateReminderSettingsRequest(
    @NotNull Boolean billRemindersEnabled,
    @NotEmpty List<Integer> billReminderDaysBefore,
    @NotNull Boolean overdueRemindersEnabled,
    @NotEmpty List<Integer> overdueReminderDays,
    @NotNull Boolean contractRemindersEnabled,
    @NotEmpty List<Integer> contractReminderDaysBefore
) {
}
