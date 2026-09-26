package com.khoa.roommanagement.reminders.settings.repository;

import com.khoa.roommanagement.reminders.settings.entity.ReminderSettings;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface ReminderSettingsRepository extends CrudRepository<ReminderSettings, Long> {

	Optional<ReminderSettings> findById(Long id);

	default ReminderSettings findSingleton() {
		return findById(ReminderSettings.SINGLETON_ID).orElseGet(ReminderSettings::createDefault);
	}
}
