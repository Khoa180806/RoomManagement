package com.khoa.roommanagement.reminders.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khoa.roommanagement.common.exception.ApiExceptionHandler;
import com.khoa.roommanagement.reminders.reminder.dto.ReminderResponse;
import com.khoa.roommanagement.reminders.reminder.entity.Reminder;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderChannel;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderStatus;
import com.khoa.roommanagement.reminders.reminder.entity.ReminderType;
import com.khoa.roommanagement.reminders.service.ReminderHistoryService;
import com.khoa.roommanagement.reminders.service.TelegramTestMessageService;
import com.khoa.roommanagement.reminders.settings.dto.ReminderSettingsResponse;
import com.khoa.roommanagement.reminders.settings.exception.InvalidReminderSettingsException;
import com.khoa.roommanagement.reminders.settings.service.ReminderSettingsService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest({ReminderController.class, ReminderSettingsController.class})
@Import(ApiExceptionHandler.class)
class ReminderSettingsAndHistoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TelegramTestMessageService telegramTestMessageService;

	@MockitoBean
	private ReminderHistoryService reminderHistoryService;

	@MockitoBean
	private ReminderSettingsService reminderSettingsService;

	@Test
	void returnsReminderSettings() throws Exception {
		Mockito.when(reminderSettingsService.getSettings()).thenReturn(new ReminderSettingsResponse(
			true, List.of(1, 3, 7), true, List.of(1, 2, 3), true, List.of(30, 60), Instant.now()));

		mockMvc.perform(get("/api/reminder-settings"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.billRemindersEnabled").value(true))
			.andExpect(jsonPath("$.billReminderDaysBefore[0]").value(1))
			.andExpect(jsonPath("$.contractReminderDaysBefore[0]").value(30));
	}

	@Test
	void updatesReminderSettings() throws Exception {
		Mockito.when(reminderSettingsService.updateSettings(Mockito.any())).thenReturn(new ReminderSettingsResponse(
			false, List.of(2, 5), true, List.of(1, 2, 3), true, List.of(30, 60), Instant.now()));

		mockMvc.perform(put("/api/reminder-settings")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "billRemindersEnabled": false,
					  "billReminderDaysBefore": [5, 2],
					  "overdueRemindersEnabled": true,
					  "overdueReminderDays": [1, 2, 3],
					  "contractRemindersEnabled": true,
					  "contractReminderDaysBefore": [60, 30]
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.billRemindersEnabled").value(false))
			.andExpect(jsonPath("$.billReminderDaysBefore[0]").value(2));
	}

	@Test
	void returnsUnprocessableForInvalidSettings() throws Exception {
		Mockito.when(reminderSettingsService.updateSettings(Mockito.any()))
			.thenThrow(new InvalidReminderSettingsException("Danh sách ngày không được trùng."));

		mockMvc.perform(put("/api/reminder-settings")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "billRemindersEnabled": true,
					  "billReminderDaysBefore": [7, 7],
					  "overdueRemindersEnabled": true,
					  "overdueReminderDays": [1, 2, 3],
					  "contractRemindersEnabled": true,
					  "contractReminderDaysBefore": [60, 30]
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.error.code").value("REMINDER_SETTINGS_INVALID"));
	}

	@Test
	void returnsReminderHistoryPage() throws Exception {
		Reminder sent = Reminder.create(ReminderType.BILL_OVERDUE, UUID.randomUUID(),
			LocalDate.of(2026, 9, 6), ReminderChannel.TELEGRAM, "Nội dung nhắc quá hạn");
		sent.markAttempt();
		sent.markSent(Instant.now());
		Mockito.when(reminderHistoryService.getReminderHistory(0, 20))
			.thenReturn(new PageImpl<>(List.of(ReminderResponse.from(sent, "2026-09", 5_437_000L)),
				PageRequest.of(0, 20), 1));

		mockMvc.perform(get("/api/reminders")
				.param("page", "0")
				.param("pageSize", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].reminderType").value("BILL_OVERDUE"))
			.andExpect(jsonPath("$.content[0].status").value("SENT"))
			.andExpect(jsonPath("$.content[0].billPeriod").value("2026-09"))
			.andExpect(jsonPath("$.content[0].billTotalAmount").value(5437000));
	}
}
