package com.khoa.roommanagement.reminders.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "reminder_settings")
public class ReminderSettings {

	/** Singleton: mọi lần đọc/ghi đều thao tác trên bản ghi id = 1. */
	public static final long SINGLETON_ID = 1L;

	/** Một quy tắc nhắc: bật/tắt kèm danh sách ngày so với mốc. */
	public record ReminderDayRule(boolean enabled, List<Integer> days) {
	}

	@Id
	private Long id;

	@Column(name = "bill_reminders_enabled", nullable = false)
	private boolean billRemindersEnabled;

	@Column(name = "bill_reminder_days_before", nullable = false, length = 64)
	private String billReminderDaysBefore;

	@Column(name = "overdue_reminders_enabled", nullable = false)
	private boolean overdueRemindersEnabled;

	@Column(name = "overdue_reminder_days", nullable = false, length = 64)
	private String overdueReminderDays;

	@Column(name = "contract_reminders_enabled", nullable = false)
	private boolean contractRemindersEnabled;

	@Column(name = "contract_reminder_days_before", nullable = false, length = 64)
	private String contractReminderDaysBefore;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ReminderSettings() {
	}

	public static ReminderSettings createDefault() {
		ReminderSettings settings = new ReminderSettings();
		settings.id = SINGLETON_ID;
		settings.billRemindersEnabled = true;
		settings.billReminderDaysBefore = "1,3,7";
		settings.overdueRemindersEnabled = true;
		settings.overdueReminderDays = "1,2,3";
		settings.contractRemindersEnabled = true;
		settings.contractReminderDaysBefore = "30,60";
		settings.updatedAt = Instant.now();
		return settings;
	}

	public List<Integer> billReminderDaysBeforeList() {
		return parseDays(billReminderDaysBefore);
	}

	public List<Integer> overdueReminderDaysList() {
		return parseDays(overdueReminderDays);
	}

	public List<Integer> contractReminderDaysBeforeList() {
		return parseDays(contractReminderDaysBefore);
	}

	public void apply(ReminderDayRule billRule, ReminderDayRule overdueRule, ReminderDayRule contractRule) {
		this.billRemindersEnabled = billRule.enabled();
		this.billReminderDaysBefore = joinDays(billRule.days());
		this.overdueRemindersEnabled = overdueRule.enabled();
		this.overdueReminderDays = joinDays(overdueRule.days());
		this.contractRemindersEnabled = contractRule.enabled();
		this.contractReminderDaysBefore = joinDays(contractRule.days());
		this.updatedAt = Instant.now();
	}

	private static List<Integer> parseDays(String raw) {
		return Arrays.stream(raw.split(","))
			.map(String::trim)
			.filter(part -> !part.isEmpty())
			.map(Integer::parseInt)
			.toList();
	}

	private static String joinDays(List<Integer> days) {
		return days.stream()
			.distinct()
			.sorted()
			.map(String::valueOf)
			.reduce((a, b) -> a + "," + b)
			.orElse("");
	}

	public Long getId() {
		return id;
	}

	public boolean isBillRemindersEnabled() {
		return billRemindersEnabled;
	}

	public boolean isOverdueRemindersEnabled() {
		return overdueRemindersEnabled;
	}

	public boolean isContractRemindersEnabled() {
		return contractRemindersEnabled;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
