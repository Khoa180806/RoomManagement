package com.khoa.roommanagement.reminders.controller;

import com.khoa.roommanagement.reminders.settings.dto.ReminderSettingsResponse;
import com.khoa.roommanagement.reminders.settings.dto.UpdateReminderSettingsRequest;
import com.khoa.roommanagement.reminders.settings.service.ReminderSettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminder-settings")
public class ReminderSettingsController {

	private final ReminderSettingsService settingsService;

	public ReminderSettingsController(ReminderSettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping
	public ReminderSettingsResponse getSettings() {
		return settingsService.getSettings();
	}

	@PutMapping
	public ReminderSettingsResponse updateSettings(@Valid @RequestBody UpdateReminderSettingsRequest request) {
		return settingsService.updateSettings(request);
	}
}
