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
