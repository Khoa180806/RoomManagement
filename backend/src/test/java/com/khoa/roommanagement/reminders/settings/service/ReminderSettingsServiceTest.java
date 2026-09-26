package com.khoa.roommanagement.reminders.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.khoa.roommanagement.reminders.settings.dto.ReminderSettingsResponse;
import com.khoa.roommanagement.reminders.settings.dto.UpdateReminderSettingsRequest;
import com.khoa.roommanagement.reminders.settings.entity.ReminderSettings;
import com.khoa.roommanagement.reminders.settings.exception.InvalidReminderSettingsException;
import com.khoa.roommanagement.reminders.settings.repository.ReminderSettingsRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderSettingsServiceTest {

	@Mock
	private ReminderSettingsRepository settingsRepository;

	private ReminderSettingsService service;

	@BeforeEach
	void setUp() {
		service = new ReminderSettingsService(settingsRepository);
	}

	@Test
	void returnsDefaultSettingsWhenMissing() {
		when(settingsRepository.findSingleton()).thenReturn(ReminderSettings.createDefault());

		ReminderSettingsResponse response = service.getSettings();

		assertThat(response.billReminderDaysBefore()).containsExactly(1, 3, 7);
		assertThat(response.overdueReminderDays()).containsExactly(1, 2, 3);
		assertThat(response.contractReminderDaysBefore()).containsExactly(30, 60);
	}

	@Test
	void updatesSettingsWithSortedDistinctDays() {
		ReminderSettings stored = ReminderSettings.createDefault();
		when(settingsRepository.findSingleton()).thenReturn(stored);
		when(settingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ReminderSettingsResponse response = service.updateSettings(new UpdateReminderSettingsRequest(
			false, List.of(3, 7, 1),
			true, List.of(2),
			true, List.of(90, 30)));

		assertThat(response.billRemindersEnabled()).isFalse();
		assertThat(response.billReminderDaysBefore()).containsExactly(1, 3, 7);
		assertThat(response.overdueReminderDays()).containsExactly(2);
		assertThat(response.contractReminderDaysBefore()).containsExactly(30, 90);
	}

	@Test
	void rejectsDuplicateDays() {
		assertThatThrownBy(() -> service.updateSettings(new UpdateReminderSettingsRequest(
			true, List.of(7, 7), true, List.of(1, 2, 3), true, List.of(60, 30))))
			.isInstanceOf(InvalidReminderSettingsException.class);
	}

	@Test
	void rejectsOutOfRangeDays() {
		assertThatThrownBy(() -> service.updateSettings(new UpdateReminderSettingsRequest(
			true, List.of(0, 7), true, List.of(1, 2, 3), true, List.of(60, 30))))
			.isInstanceOf(InvalidReminderSettingsException.class);
	}

	@Test
	void rejectsEmptyLists() {
		assertThatThrownBy(() -> service.updateSettings(new UpdateReminderSettingsRequest(
			true, List.of(), true, List.of(1, 2, 3), true, List.of(60, 30))))
			.isInstanceOf(InvalidReminderSettingsException.class);
	}
}
