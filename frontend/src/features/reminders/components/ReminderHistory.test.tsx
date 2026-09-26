import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ReminderHistory } from "./ReminderHistory";
import type { Reminder, ReminderPage } from "../api";

function reminderOf(id: string, type: Reminder["reminderType"], status: Reminder["status"]): Reminder {
  return {
    id,
    reminderType: type,
    referenceId: `ref-${id}`,
    targetDate: "2026-09-06",
    channel: "TELEGRAM",
    status,
    attemptCount: 1,
    sentAt: "2026-09-06T02:00:00Z",
    errorCode: status === "FAILED" ? "TELEGRAM_SEND_FAILED" : null,
    createdAt: "2026-09-06T02:00:00Z",
    billPeriod: type.startsWith("BILL") ? "2026-09" : null,
    billTotalAmount: type.startsWith("BILL") ? 5_437_000 : null,
  };
}

function pageOf(content: Reminder[], totalPages: number): ReminderPage {
  return {
    content,
    number: 0,
    size: 6,
    totalElements: content.length,
    totalPages,
  };
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("ReminderHistory", () => {
  it("renders reminders from the server with status and bill info", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
      new Response(JSON.stringify(pageOf([
        reminderOf("r1", "BILL_OVERDUE", "SENT"),
        reminderOf("r2", "CONTRACT_TERMINATED", "FAILED"),
      ], 1)), { status: 200, headers: { "Content-Type": "application/json" } }),
    ));

    render(<ReminderHistory />);

    expect(await screen.findByText("Nhắc quá hạn")).toBeInTheDocument();
    expect(screen.getByText("Đã gửi")).toBeInTheDocument();
    expect(screen.getByText("Thất bại")).toBeInTheDocument();
    expect(screen.getByText("Lỗi: TELEGRAM_SEND_FAILED")).toBeInTheDocument();
    expect(screen.getByText("5.437.000 ₫")).toBeInTheDocument();
  });

  it("requests the next page from the server", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(pageOf([reminderOf("r1", "BILL_UPCOMING", "SENT")], 2)), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<ReminderHistory />);

    await waitFor(() => expect(screen.getByRole("navigation", { name: "Phân trang" })).toBeInTheDocument());
    await user.click(screen.getByRole("button", { name: "Sau →" }));

    expect(fetchMock).toHaveBeenLastCalledWith(
      expect.stringContaining("/api/reminders?page=1"),
      expect.anything(),
    );
  });
});
