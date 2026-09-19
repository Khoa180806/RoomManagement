import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { BillCard } from "./BillList";
import type { Bill } from "../api";

const bill: Bill = {
  id: "bill-1",
  contractId: "contract-1",
  period: "2026-09",
  rentAmount: 4400000,
  electricityUnitPrice: 3800,
  waterFee: 200000,
  serviceFee: 100000,
  oldMeterValue: 4321,
  newMeterValue: 4515,
  consumption: 194,
  electricityAmount: 737000,
  totalAmount: 5437000,
  status: "PENDING",
  dueDate: "2026-09-05",
  createdAt: "2026-09-01T00:00:00Z",
};

describe("BillCard", () => {
  it("starts collapsed and toggles the bill details", async () => {
    const user = userEvent.setup();
    render(<BillCard bill={bill} />);

    const toggle = screen.getByRole("button", { name: /mở chi tiết/i });
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    expect(screen.queryByText("Tiền điện (194 kWh × 3.800 ₫/kWh)")).not.toBeInTheDocument();

    await user.click(toggle);
    expect(toggle).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText("Tiền điện (194 kWh × 3.800 ₫/kWh)")).toBeInTheDocument();
    expect(screen.getByText("Hạn thanh toán:")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /thu gọn chi tiết/i }));
    expect(screen.getByRole("button", { name: /mở chi tiết/i })).toHaveAttribute("aria-expanded", "false");
  });
});
