import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PaymentHistory } from "./PaymentHistory";
import type { Payment } from "../api";

function paymentOf(id: string, period: string, onTime: boolean, receiptId: string | null): Payment {
  return {
    id,
    bill: {
      id: `bill-${id}`,
      period,
      totalAmount: 5_000_000,
      dueDate: `${period}-05`,
      status: "PAID",
    },
    paidAt: `${period}-05T12:00:00.000Z`,
    note: null,
    onTime,
    createdAt: `${period}-06T00:00:00.000Z`,
    receipt: receiptId ? { id: receiptId, contentType: "image/jpeg", fileSize: 1024 } : null,
  };
}

const historyProps = {
  currentPage: 0,
  totalPages: 1,
  onTime: undefined,
  onPageChange: vi.fn(),
  onTimeChange: vi.fn(),
  isLoading: false,
};

describe("PaymentHistory", () => {
  it("shows receipt links coming from the API without any upload", () => {
    const payments = [
      paymentOf("p1", "2026-08", true, "receipt-1"),
      paymentOf("p2", "2026-07", true, null),
    ];

    render(<PaymentHistory payments={payments} {...historyProps} />);

    expect(screen.getByRole("link", { name: "Mở ảnh" })).toHaveAttribute("href", "/api/receipts/receipt-1");
    expect(screen.getByText("Thêm chứng từ")).toBeInTheDocument();
  });

  it("shows six server-provided items and requests the next page", async () => {
    const user = userEvent.setup();
    const onPageChange = vi.fn();
    const payments = Array.from({ length: 6 }, (_, index) =>
      paymentOf(String(index + 1), `2026-${String(7 - index).padStart(2, "0")}`, true, null));

    render(<PaymentHistory payments={payments} {...historyProps} totalPages={2} onPageChange={onPageChange} />);

    expect(screen.getAllByRole("listitem")).toHaveLength(3);
    await user.click(screen.getByRole("button", { name: "▼ Xem tất cả" }));
    expect(screen.getAllByRole("listitem")).toHaveLength(6);
    expect(screen.getByRole("navigation", { name: "Phân trang" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Sau →" }));
    expect(onPageChange).toHaveBeenCalledWith(1);
  });
});
