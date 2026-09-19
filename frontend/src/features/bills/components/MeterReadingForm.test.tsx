import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { MeterReadingForm } from "./MeterReadingForm";
import type { ElectricityReading } from "../api";

const readings: ElectricityReading[] = Array.from({ length: 7 }, (_, index) => ({
  id: String(index + 1),
  contractId: "contract-1",
  period: `2025-${String(7 - index).padStart(2, "0")}`,
  meterValue: 100 + index,
  recordedAt: "2025-07-01T00:00:00Z",
}));

describe("MeterReadingForm history", () => {
  it("shows six readings per page after expanding history", async () => {
    const user = userEvent.setup();

    render(
      <MeterReadingForm
        readings={readings}
        period="2025-07"
        meterValue=""
        error={null}
        isSaving={false}
        onPeriodChange={vi.fn()}
        onMeterValueChange={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    await user.click(screen.getByRole("button", { name: "▼ Xem tất cả" }));

    expect(screen.getAllByRole("listitem")).toHaveLength(6);
    expect(screen.getByRole("navigation", { name: "Phân trang" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Sau →" }));
    expect(screen.getAllByRole("listitem")).toHaveLength(1);
    expect(screen.getByText("2025-01")).toBeInTheDocument();
  });
});
