import { request } from "../../shared/api/client";

export type Reminder = {
  id: string;
  reminderType: "BILL_UPCOMING" | "BILL_OVERDUE" | "CONTRACT_EXPIRING" | "CONTRACT_TERMINATED";
  referenceId: string;
  targetDate: string;
  channel: string;
  status: "SENT" | "FAILED";
  attemptCount: number;
  sentAt: string | null;
  errorCode: string | null;
  createdAt: string;
  billPeriod: string | null;
  billTotalAmount: number | null;
};

export type ReminderPage = {
  content: Reminder[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export async function getReminders(page = 0, pageSize = 6): Promise<ReminderPage> {
  const params = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  return request<ReminderPage>(`/api/reminders?${params.toString()}`);
}
