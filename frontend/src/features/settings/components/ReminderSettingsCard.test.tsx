import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ReminderSettingsCard } from "./ReminderSettingsCard";

function fetchSequence(responses: Array<{ status: number; body: unknown }>) {
  const responsesCopy = [...responses];
  return vi.fn().mockImplementation(() => {
    const next = responsesCopy.shift() ?? responses[responses.length - 1];
    return Promise.resolve(
      new Response(JSON.stringify(next.body), {
        status: next.status,
        headers: { "Content-Type": "application/json" },
      }),
    );
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

const defaultSettings = {
  billRemindersEnabled: true,
  billReminderDaysBefore: [1, 3, 7],
  overdueRemindersEnabled: true,
  overdueReminderDays: [1, 2, 3],
  contractRemindersEnabled: true,
  contractReminderDaysBefore: [30, 60],
  updatedAt: "2026-09-26T00:00:00Z",
};

describe("ReminderSettingsCard", () => {
  it("loads settings and saves toggled days", async () => {
    const user = userEvent.setup();
    const fetchMock = fetchSequence([
      { status: 200, body: defaultSettings },
      { status: 200, body: { ...defaultSettings, billReminderDaysBefore: [1, 7] } },
    ]);
    vi.stubGlobal("fetch", fetchMock);

    render(<ReminderSettingsCard />);

    await waitFor(() => expect(screen.getByRole("button", { name: "Lưu cấu hình" })).toBeEnabled());
    const upcomingGroup = screen.getByRole("group", { name: "Gửi trước hạn" });
    expect(within(upcomingGroup).getByRole("button", { name: "3 ngày", pressed: true })).toBeInTheDocument();

    await user.click(within(upcomingGroup).getByRole("button", { name: "3 ngày" }));
    await user.click(screen.getByRole("button", { name: "Lưu cấu hình" }));

    await waitFor(() => expect(screen.getByText("Đã lưu cấu hình nhắc.")).toBeInTheDocument());
    const putCall = fetchMock.mock.calls.find(([url, init]) => url === "/api/reminder-settings" && (init as RequestInit).method === "PUT");
    expect(putCall).toBeDefined();
    expect(JSON.parse((putCall![1] as RequestInit).body as string).billReminderDaysBefore).toEqual([1, 7]);
  });

  it("shows the server error when saving fails", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", fetchSequence([
      { status: 200, body: defaultSettings },
      { status: 422, body: { error: { code: "REMINDER_SETTINGS_INVALID", message: "Danh sách ngày không được trùng.", details: [] } } },
    ]));

    render(<ReminderSettingsCard />);

    await waitFor(() => expect(screen.getByRole("button", { name: "Lưu cấu hình" })).toBeEnabled());
    await user.click(screen.getByRole("button", { name: "Lưu cấu hình" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Danh sách ngày không được trùng.");
  });
});
