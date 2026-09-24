package com.khoa.roommanagement.reminders.controller;

import com.khoa.roommanagement.reminders.service.TelegramTestMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final TelegramTestMessageService telegramTestMessageService;

    public ReminderController(TelegramTestMessageService telegramTestMessageService) {
        this.telegramTestMessageService = telegramTestMessageService;
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.OK)
    public SendTestMessageResponse sendTestMessage() {
        telegramTestMessageService.sendTestMessage();
        return new SendTestMessageResponse(true);
    }

    public record SendTestMessageResponse(boolean sent) {
    }
}
