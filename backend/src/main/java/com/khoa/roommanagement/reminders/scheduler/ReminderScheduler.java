package com.khoa.roommanagement.reminders.scheduler;

import com.khoa.roommanagement.reminders.service.ReminderDispatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job hằng ngày lúc 09:00 theo Asia/Ho_Chi_Minh. Cron có thể ghi đè qua
 * biến môi trường APP_REMINDERS_CRON khi cần kiểm thử thủ công.
 */
@Component
public class ReminderScheduler {

	private final ReminderDispatchService dispatchService;

	public ReminderScheduler(ReminderDispatchService dispatchService) {
		this.dispatchService = dispatchService;
	}

	@Scheduled(cron = "${app.reminders.cron:0 0 9 * * *}", zone = "Asia/Ho_Chi_Minh")
	public void runDailyReminderJob() {
		dispatchService.runDailyJob();
	}
}
