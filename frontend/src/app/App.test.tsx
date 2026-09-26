import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "./App";
import { useRentalWorkspace } from "./useRentalWorkspace";
import { DashboardPage } from "../features/dashboard/pages/DashboardPage";
import { BillsPage } from "../features/bills/pages/BillsPage";
import { PaymentsPage } from "../features/payments/pages/PaymentsPage";
import { SettingsPage } from "../features/settings/pages/SettingsPage";
import type { RentalContract } from "../features/contracts/api";

vi.mock("./useRentalWorkspace", () => ({
  useRentalWorkspace: vi.fn(),
}));

vi.mock("../features/auth/SessionContext", () => ({
  useSession: () => ({
    status: "authenticated",
    markAuthenticated: vi.fn(),
    markUnauthenticated: vi.fn(),
  }),
}));

vi.mock("../features/reminders/components/ReminderHistory", () => ({
  ReminderHistory: () => <p data-testid="reminder-history">ReminderHistory</p>,
}));
vi.mock("../features/settings/components/TelegramSettingsCard", () => ({
  TelegramSettingsCard: () => <p data-testid="telegram-card">TelegramSettingsCard</p>,
}));
vi.mock("../features/settings/components/ReminderSettingsCard", () => ({
  ReminderSettingsCard: () => <p data-testid="reminder-settings">ReminderSettingsCard</p>,
}));

const contract: RentalContract = {
  id: "c1",
  startDate: "2026-02-02",
  endDate: "2027-02-02",
  paymentDueDay: 5,
  rentAmount: 4_400_000,
  electricityUnitPrice: 3_800,
  waterFee: 200_000,
  serviceFee: 100_000,
  status: "ACTIVE",
};

function stubWorkspace(overrides: Record<string, unknown> = {}) {
  vi.mocked(useRentalWorkspace).mockReturnValue({
    contract,
    form: {
      startDate: "", endDate: "", paymentDueDay: "4", rentAmount: "",
      electricityUnitPrice: "", waterFee: "", serviceFee: "",
    },
    isLoading: false,
    isSaving: false,
    error: null,
    readings: [],
    readingPeriod: "2026-09",
    readingMeterValue: "",
    readingError: null,
    isRecordingReading: false,
    bills: [],
    billPeriod: "2026-09",
    billError: null,
    isCreatingBill: false,
    payments: [],
    paymentPage: 0,
    paymentTotalPages: 1,
    paymentOnTime: undefined,
    isLoadingPayments: false,
    selectedBillId: null,
    paidAt: "",
    paymentNote: "",
    paymentError: null,
    isConfirmingPayment: false,
    updateField: vi.fn(),
    submitContract: vi.fn(),
    recordReading: vi.fn(),
    createNewBill: vi.fn(),
    confirmPaymentHandler: vi.fn(),
    setReadingPeriod: vi.fn(),
    setReadingMeterValue: vi.fn(),
    setBillPeriod: vi.fn(),
    setSelectedBillId: vi.fn(),
    setPaidAt: vi.fn(),
    setPaymentNote: vi.fn(),
    setPaymentPage: vi.fn(),
    setPaymentFilter: vi.fn(),
    ...overrides,
  } as never);
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/app" element={<App />}>
          <Route index element={<DashboardPage />} />
          <Route path="bills" element={<BillsPage />} />
          <Route path="payments" element={<PaymentsPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe("App shell", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    stubWorkspace();
  });

  it("shows the dashboard with overview stats by default", () => {
    renderAt("/app");

    expect(screen.getByRole("heading", { name: "Tổng quan" })).toBeInTheDocument();
    expect(screen.getByText("0 kỳ")).toBeInTheDocument();
    const nav = screen.getByRole("navigation", { name: "Menu điều hướng" });
    expect(within(nav).getByRole("link", { name: "Hóa đơn" })).toBeInTheDocument();
  });

  it("navigates to the bills page from the nav", async () => {
    const user = userEvent.setup();
    renderAt("/app");

    const desktopNav = screen.getByRole("navigation", { name: "Menu điều hướng" });
    await user.click(within(desktopNav).getByRole("link", { name: "Hóa đơn" }));

    expect(screen.getByRole("heading", { name: "Ghi chỉ số điện" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Tạo và xem hóa đơn" })).toBeInTheDocument();
  });

  it("shows the settings page with contract and reminder cards", () => {
    renderAt("/app/settings");

    expect(screen.getByRole("heading", { name: "Hợp đồng của bạn" })).toBeInTheDocument();
    expect(screen.getByTestId("telegram-card")).toBeInTheDocument();
    expect(screen.getByTestId("reminder-settings")).toBeInTheDocument();
    expect(screen.getByTestId("reminder-history")).toBeInTheDocument();
  });

  it("shows the contract setup form when no contract exists", () => {
    stubWorkspace({ contract: null });

    renderAt("/app");

    expect(
      screen.getAllByRole("heading", { name: "Tạo hợp đồng đầu tiên" }).length,
    ).toBeGreaterThan(0);
    expect(screen.queryByRole("heading", { name: "Tổng quan" })).not.toBeInTheDocument();
  });
});
