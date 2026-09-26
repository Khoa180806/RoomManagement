package com.khoa.roommanagement.reminders.settings.repository;

import com.khoa.roommanagement.reminders.settings.entity.ReminderSettings;
import org.springframework.data.repository.CrudRepository;

public interface ReminderSettingsRepository extends CrudRepository<ReminderSettings, Long> {

	default ReminderSettings findSingleton() {
		return findById(ReminderSettings.SINGLETON_ID).orElseGet(ReminderSettings::createDefault);
	}
}
