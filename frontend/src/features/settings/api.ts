import { request } from "../../shared/api/client";

export type SendTestMessageResult = {
  sent: boolean;
};

export async function sendTestTelegramMessage(): Promise<SendTestMessageResult> {
  return request<SendTestMessageResult>("/api/reminders/test", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
  });
}

export type ReminderSettings = {
  billRemindersEnabled: boolean;
  billReminderDaysBefore: number[];
  overdueRemindersEnabled: boolean;
  overdueReminderDays: number[];
  contractRemindersEnabled: boolean;
  contractReminderDaysBefore: number[];
  updatedAt: string;
};

export async function getReminderSettings(): Promise<ReminderSettings> {
  return request<ReminderSettings>("/api/reminder-settings");
}

export async function updateReminderSettings(
  settings: Omit<ReminderSettings, "updatedAt">,
): Promise<ReminderSettings> {
  return request<ReminderSettings>("/api/reminder-settings", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(settings),
  });
}
