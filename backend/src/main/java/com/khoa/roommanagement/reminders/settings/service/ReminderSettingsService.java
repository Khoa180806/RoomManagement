package com.khoa.roommanagement.reminders.settings.service;

import com.khoa.roommanagement.reminders.settings.dto.ReminderSettingsResponse;
import com.khoa.roommanagement.reminders.settings.dto.UpdateReminderSettingsRequest;
import com.khoa.roommanagement.reminders.settings.entity.ReminderSettings;
import com.khoa.roommanagement.reminders.settings.exception.InvalidReminderSettingsException;
import com.khoa.roommanagement.reminders.settings.repository.ReminderSettingsRepository;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderSettingsService {

	private static final int MAX_DAYS = 365;
	private static final int MAX_ENTRIES = 5;

	private final ReminderSettingsRepository settingsRepository;

	public ReminderSettingsService(ReminderSettingsRepository settingsRepository) {
		this.settingsRepository = settingsRepository;
	}

	@Transactional
	public ReminderSettingsResponse getSettings() {
		return toResponse(settingsRepository.findSingleton());
	}

	@Transactional
	public ReminderSettingsResponse updateSettings(UpdateReminderSettingsRequest request) {
		ReminderSettings.ReminderDayRule billRule = new ReminderSettings.ReminderDayRule(
			request.billRemindersEnabled(), normalizeDays(request.billReminderDaysBefore(), "nhắc trước hạn thanh toán"));
		ReminderSettings.ReminderDayRule overdueRule = new ReminderSettings.ReminderDayRule(
			request.overdueRemindersEnabled(), normalizeDays(request.overdueReminderDays(), "nhắc quá hạn"));
		ReminderSettings.ReminderDayRule contractRule = new ReminderSettings.ReminderDayRule(
			request.contractRemindersEnabled(), normalizeDays(request.contractReminderDaysBefore(), "nhắc hết hợp đồng"));

		ReminderSettings settings = settingsRepository.findSingleton();
		settings.apply(billRule, overdueRule, contractRule);
		return toResponse(settingsRepository.save(settings));
	}

	/**
	 * Chuẩn hóa danh sách ngày: số dương, không trùng, tối đa 5 mục và không
	 * vượt quá 365 ngày.
	 */
	private List<Integer> normalizeDays(List<Integer> days, String label) {
		if (days == null || days.isEmpty()) {
			throw new InvalidReminderSettingsException("Danh sách ngày " + label + " là bắt buộc.");
		}
		if (days.size() > MAX_ENTRIES) {
			throw new InvalidReminderSettingsException("Danh sách ngày " + label + " tối đa " + MAX_ENTRIES + " mục.");
		}
		for (Integer day : days) {
			if (day == null || day < 1 || day > MAX_DAYS) {
				throw new InvalidReminderSettingsException(
					"Ngày " + label + " phải từ 1 đến " + MAX_DAYS + ".");
			}
		}
		if (new HashSet<>(days).size() != days.size()) {
			throw new InvalidReminderSettingsException("Danh sách ngày " + label + " không được trùng.");
		}
		return days.stream().distinct().sorted().toList();
	}

	private ReminderSettingsResponse toResponse(ReminderSettings settings) {
		return new ReminderSettingsResponse(
			settings.isBillRemindersEnabled(),
			settings.billReminderDaysBeforeList(),
			settings.isOverdueRemindersEnabled(),
			settings.overdueReminderDaysList(),
			settings.isContractRemindersEnabled(),
			settings.contractReminderDaysBeforeList(),
			settings.getUpdatedAt()
		);
	}
}
