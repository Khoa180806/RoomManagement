package com.khoa.roommanagement.reminders.controller;

import com.khoa.roommanagement.reminders.reminder.dto.ReminderResponse;
import com.khoa.roommanagement.reminders.service.ReminderHistoryService;
import com.khoa.roommanagement.reminders.service.TelegramTestMessageService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final TelegramTestMessageService telegramTestMessageService;
    private final ReminderHistoryService reminderHistoryService;

    public ReminderController(
        TelegramTestMessageService telegramTestMessageService,
        ReminderHistoryService reminderHistoryService
    ) {
        this.telegramTestMessageService = telegramTestMessageService;
        this.reminderHistoryService = reminderHistoryService;
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.OK)
    public SendTestMessageResponse sendTestMessage() {
        telegramTestMessageService.sendTestMessage();
        return new SendTestMessageResponse(true);
    }

    @GetMapping
    public Page<ReminderResponse> getReminderHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Integer pageSize
    ) {
        return reminderHistoryService.getReminderHistory(page, pageSize);
    }

    public record SendTestMessageResponse(boolean sent) {
    }
}
