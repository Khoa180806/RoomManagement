import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TelegramSettingsCard } from "./TelegramSettingsCard";

function stubFetch(status: number, body: unknown) {
  return vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("TelegramSettingsCard", () => {
  it("shows a success status after the test message is sent", async () => {
    const user = userEvent.setup();
    const fetchMock = stubFetch(200, { sent: true });
    vi.stubGlobal("fetch", fetchMock);

    render(<TelegramSettingsCard />);

    await user.click(screen.getByRole("button", { name: "Gửi tin nhắn thử" }));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/reminders/test",
      expect.objectContaining({ method: "POST" }),
    );
    expect(screen.getByText("Đã gửi tin nhắn thử. Hãy kiểm tra chat Telegram của bạn.")).toBeInTheDocument();
  });

  it("shows the server error when Telegram is not configured", async () => {
    const user = userEvent.setup();
    vi.stubGlobal(
      "fetch",
      stubFetch(409, {
        error: { code: "TELEGRAM_NOT_CONFIGURED", message: "Chưa cấu hình Telegram.", details: [] },
      }),
    );

    render(<TelegramSettingsCard />);

    await user.click(screen.getByRole("button", { name: "Gửi tin nhắn thử" }));

    expect(screen.getByRole("alert")).toHaveTextContent("Chưa cấu hình Telegram.");
    expect(screen.queryByText("Đã gửi tin nhắn thử. Hãy kiểm tra chat Telegram của bạn.")).not.toBeInTheDocument();
  });
});
