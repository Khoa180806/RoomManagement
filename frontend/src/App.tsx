import { useEffect, useState } from "react";
import type { FormEvent, InputHTMLAttributes } from "react";
import {
  createRentalContract,
  getActiveRentalContract,
  type RentalContract,
  type RentalContractInput,
} from "./features/contracts/api";
import {
  recordElectricityReading,
  getElectricityReadings,
  createBill,
  getBills,
  type ElectricityReading,
  type Bill,
} from "./features/bills/api";
import {
  confirmPayment,
  getPayments,
  type Payment,
} from "./features/payments/api";
import {
  uploadReceipt,
  getReceiptUrl,
  type Receipt,
} from "./features/receipts/api";
import "./App.css";

const VIETNAMESE_MONTHS = [
  "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
  "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
  "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12",
];

const MIN_YEAR = 2024;
const MAX_YEAR = new Date().getFullYear() + 5;

function parsePeriod(period: string): { year: string; month: string } {
  if (/^\d{4}-\d{2}$/.test(period)) {
    const [year, month] = period.split("-");
    return { year, month };
  }
  return { year: "", month: "" };
}

function parseDate(dateStr: string): { year: string; month: string; day: string } {
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
    const [year, month, day] = dateStr.split("-");
    return { year, month, day };
  }
  return { year: "", month: "", day: "" };
}

function makePeriod(year: string, month: string): string {
  if (!year || !month) return "";
  return `${year}-${month.padStart(2, "0")}`;
}

function makeDate(year: string, month: string, day: string): string {
  if (!year || !month || !day) return "";
  return `${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
}

function getYears(): string[] {
  const years: string[] = [];
  for (let y = MIN_YEAR; y <= MAX_YEAR; y++) {
    years.push(String(y));
  }
  return years;
}

function getDays(): string[] {
  const days: string[] = [];
  for (let d = 1; d <= 31; d++) {
    days.push(String(d));
  }
  return days;
}

const initialForm: RentalContractInput = {
  startDate: "",
  endDate: "",
  paymentDueDay: "4",
  rentAmount: "",
  electricityUnitPrice: "",
  waterFee: "",
  serviceFee: "",
};
const money = new Intl.NumberFormat("vi-VN", {
  style: "currency",
  currency: "VND",
  maximumFractionDigits: 0,
});

function getCurrentPeriod(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

function App() {
  const [contract, setContract] = useState<RentalContract | null>(null);
  const [form, setForm] = useState(initialForm);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Meter reading state
  const [readings, setReadings] = useState<ElectricityReading[]>([]);
  const [readingPeriod, setReadingPeriod] = useState(getCurrentPeriod);
  const [readingMeterValue, setReadingMeterValue] = useState("");
  const [readingError, setReadingError] = useState<string | null>(null);
  const [isRecordingReading, setIsRecordingReading] = useState(false);

  // Bill state
  const [bills, setBills] = useState<Bill[]>([]);
  const [billPeriod, setBillPeriod] = useState(getCurrentPeriod);
  const [billError, setBillError] = useState<string | null>(null);
  const [isCreatingBill, setIsCreatingBill] = useState(false);

  // Payment state
  const [payments, setPayments] = useState<Payment[]>([]);
  const [selectedBillId, setSelectedBillId] = useState<string | null>(null);
  const [paidAt, setPaidAt] = useState("");
  const [paymentNote, setPaymentNote] = useState("");
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [isConfirmingPayment, setIsConfirmingPayment] = useState(false);

  useEffect(() => {
    getActiveRentalContract()
      .then((c) => {
        setContract(c);
        return Promise.all([getElectricityReadings(), getBills(), getPayments()]);
      })
      .then(([readingsData, billsData, paymentsData]) => {
        setReadings(readingsData);
        setBills(billsData.content);
        setPayments(paymentsData.content);
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof Error && requestError.message !== "NOT_FOUND")
          setError("Không thể tải dữ liệu. Hãy thử lại sau.");
      })
      .finally(() => setIsLoading(false));
  }, []);

  function updateField(name: keyof RentalContractInput, value: string) {
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    const errors: string[] = [];
    if (!form.startDate) errors.push("Ngày bắt đầu là bắt buộc");
    if (!form.endDate) errors.push("Ngày kết thúc là bắt buộc");
    if (form.startDate && form.endDate && form.endDate <= form.startDate) {
      errors.push("Ngày kết thúc phải sau ngày bắt đầu");
    }
    if (!form.paymentDueDay || Number(form.paymentDueDay) < 1 || Number(form.paymentDueDay) > 28) {
      errors.push("Ngày đến hạn phải từ 1 đến 28");
    }
    if (form.rentAmount === "" || Number(form.rentAmount) < 0) {
      errors.push("Tiền phòng không được âm");
    }
    if (form.electricityUnitPrice === "" || Number(form.electricityUnitPrice) < 0) {
      errors.push("Đơn giá điện không được âm");
    }
    if (form.waterFee === "" || Number(form.waterFee) < 0) {
      errors.push("Tiền nước không được âm");
    }
    if (form.serviceFee === "" || Number(form.serviceFee) < 0) {
      errors.push("Phí dịch vụ không được âm");
    }

    if (errors.length > 0) {
      setError(errors.join(". "));
      return;
    }

    setIsSaving(true);
    try {
      const c = await createRentalContract(form);
      setContract(c);
    } catch (requestError: unknown) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Không thể lưu hợp đồng.",
      );
    } finally {
      setIsSaving(false);
    }
  }

  async function recordReading(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setReadingError(null);

    const errors: string[] = [];
    if (!readingPeriod) errors.push("Kỳ là bắt buộc");
    if (!/^\d{4}-\d{2}$/.test(readingPeriod)) {
      errors.push("Kỳ phải có định dạng YYYY-MM");
    }
    if (readingMeterValue === "" || Number(readingMeterValue) < 0) {
      errors.push("Chỉ số điện không được âm");
    }

    if (errors.length > 0) {
      setReadingError(errors.join(". "));
      return;
    }

    setIsRecordingReading(true);
    try {
      const reading = await recordElectricityReading({
        period: readingPeriod,
        meterValue: readingMeterValue,
      });
      setReadings((prev) => [reading, ...prev]);
      setReadingMeterValue("");
    } catch (requestError: unknown) {
      setReadingError(
        requestError instanceof Error
          ? requestError.message
          : "Không thể ghi chỉ số.",
      );
    } finally {
      setIsRecordingReading(false);
    }
  }

  async function createNewBill(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBillError(null);

    const errors: string[] = [];
    if (!billPeriod) errors.push("Kỳ là bắt buộc");
    if (!/^\d{4}-\d{2}$/.test(billPeriod)) {
      errors.push("Kỳ phải có định dạng YYYY-MM");
    }

    if (errors.length > 0) {
      setBillError(errors.join(". "));
      return;
    }

    setIsCreatingBill(true);
    try {
      const bill = await createBill(billPeriod);
      setBills((prev) => [bill, ...prev]);
    } catch (requestError: unknown) {
      setBillError(
        requestError instanceof Error
          ? requestError.message
          : "Không thể tạo hóa đơn.",
      );
    } finally {
      setIsCreatingBill(false);
    }
  }

  async function confirmPaymentHandler(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPaymentError(null);

    if (!selectedBillId) {
      setPaymentError("Vui lòng chọn hóa đơn để thanh toán.");
      return;
    }

    if (!paidAt) {
      setPaymentError("Ngày thanh toán là bắt buộc.");
      return;
    }

    setIsConfirmingPayment(true);
    try {
      const payment = await confirmPayment(selectedBillId, {
        paidAt: new Date(paidAt).toISOString(),
        note: paymentNote || undefined,
        idempotencyKey: `payment-${selectedBillId}-${Date.now()}`,
      });
      setPayments((prev) => [payment, ...prev]);
      setBills((prev) => prev.map(b => 
        b.id === selectedBillId ? { ...b, status: "PAID" } : b
      ));
      setSelectedBillId(null);
      setPaidAt("");
      setPaymentNote("");
    } catch (requestError: unknown) {
      setPaymentError(
        requestError instanceof Error
          ? requestError.message
          : "Không thể xác nhận thanh toán.",
      );
    } finally {
      setIsConfirmingPayment(false);
    }
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <a className="brand" href="#main-content">
          <span className="brand-mark">RM</span>
          <span>Nhà trọ của tôi</span>
        </a>
        <span className="topbar-label">Quản lý phòng trọ</span>
      </header>
      <section className="hero-band" aria-labelledby="page-title">
        <p className="eyebrow">Hợp đồng thuê</p>
        <h1 id="page-title">
          Biết rõ từng khoản,
          <br />
          an tâm mỗi kỳ.
        </h1>
        <p className="hero-copy">
          Lưu cấu hình một lần để tiền phòng, điện, nước và dịch vụ luôn được
          tính đúng.
        </p>
      </section>
      <div id="main-content" className="content-grid" tabIndex={-1}>
        {isLoading ? (
          <LoadingState />
        ) : contract ? (
          <>
            <ContractCard contract={contract} />
            <MeterReadingForm
              readings={readings}
              period={readingPeriod}
              meterValue={readingMeterValue}
              error={readingError}
              isSaving={isRecordingReading}
              onPeriodChange={setReadingPeriod}
              onMeterValueChange={setReadingMeterValue}
              onSubmit={recordReading}
            />
            <BillForm
              bills={bills}
              period={billPeriod}
              error={billError}
              isSaving={isCreatingBill}
              onPeriodChange={setBillPeriod}
              onSubmit={createNewBill}
            />
            <PaymentForm
              bills={bills}
              payments={payments}
              selectedBillId={selectedBillId}
              paidAt={paidAt}
              note={paymentNote}
              error={paymentError}
              isSaving={isConfirmingPayment}
              onBillSelect={setSelectedBillId}
              onPaidAtChange={setPaidAt}
              onNoteChange={setPaymentNote}
              onSubmit={confirmPaymentHandler}
            />
          </>
        ) : (
          <ContractForm
            form={form}
            error={error}
            isSaving={isSaving}
            onSubmit={submit}
            onChange={updateField}
          />
        )}
        <aside className="rule-card" aria-labelledby="rule-title">
          <span className="rule-icon" aria-hidden="true">
            !
          </span>
          <div>
            <h2 id="rule-title">Quy tắc thanh toán</h2>
            <p>
              Hạn đóng tiền được cố định theo hợp đồng. Trễ quá 3 ngày lịch, hợp
              đồng sẽ bị hủy.
            </p>
          </div>
        </aside>
      </div>
    </main>
  );
}

/* ─── Custom Vietnamese Select Components ─── */

function MonthSelect({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  const { year, month } = parsePeriod(value);
  const years = getYears();

  function handleYearChange(e: React.ChangeEvent<HTMLSelectElement>) {
    onChange(makePeriod(e.target.value, month));
  }

  function handleMonthChange(e: React.ChangeEvent<HTMLSelectElement>) {
    onChange(makePeriod(year, e.target.value));
  }

  return (
    <div className="period-select">
      <select
        value={year}
        onChange={handleYearChange}
        aria-label="Năm"
      >
        <option value="">Năm</option>
        {years.map((y) => (
          <option key={y} value={y}>{y}</option>
        ))}
      </select>
      <select
        value={month}
        onChange={handleMonthChange}
        aria-label="Tháng"
      >
        <option value="">Tháng</option>
        {VIETNAMESE_MONTHS.map((name, i) => (
          <option key={i + 1} value={String(i + 1)}>{name}</option>
        ))}
      </select>
    </div>
  );
}

function DateSelect({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  const { year, month, day } = parseDate(value);
  const years = getYears();
  const days = getDays();

  function handleYearChange(e: React.ChangeEvent<HTMLSelectElement>) {
    onChange(makeDate(e.target.value, month, day));
  }

  function handleMonthChange(e: React.ChangeEvent<HTMLSelectElement>) {
    onChange(makeDate(year, e.target.value, day));
  }

  function handleDayChange(e: React.ChangeEvent<HTMLSelectElement>) {
    onChange(makeDate(year, month, e.target.value));
  }

  return (
    <div className="date-select">
      <select
        value={day}
        onChange={handleDayChange}
        aria-label="Ngày"
      >
        <option value="">Ngày</option>
        {days.map((d) => (
          <option key={d} value={d}>{d}</option>
        ))}
      </select>
      <select
        value={month}
        onChange={handleMonthChange}
        aria-label="Tháng"
      >
        <option value="">Tháng</option>
        {VIETNAMESE_MONTHS.map((name, i) => (
          <option key={i + 1} value={String(i + 1)}>{name}</option>
        ))}
      </select>
      <select
        value={year}
        onChange={handleYearChange}
        aria-label="Năm"
      >
        <option value="">Năm</option>
        {years.map((y) => (
          <option key={y} value={y}>{y}</option>
        ))}
      </select>
    </div>
  );
}

/* ─── Existing Components ─── */

function ContractForm({
  form,
  error,
  isSaving,
  onSubmit,
  onChange,
}: {
  form: RentalContractInput;
  error: string | null;
  isSaving: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onChange: (name: keyof RentalContractInput, value: string) => void;
}) {
  return (
    <section className="contract-panel" aria-labelledby="form-title">
      <div className="panel-heading">
        <div>
          <p className="section-label">Bước 1 / 1</p>
          <h2 id="form-title">Tạo hợp đồng đầu tiên</h2>
        </div>
        <span className="required-note">
          <b>*</b> Bắt buộc
        </span>
      </div>
      <form onSubmit={onSubmit} noValidate>
        <fieldset>
          <legend>Thời hạn</legend>
          <div className="field-grid two-cols">
            <label className="field" htmlFor="start-date">
              <span>Ngày bắt đầu <b aria-hidden="true">*</b></span>
              <DateSelect
                value={form.startDate}
                onChange={(v) => onChange("startDate", v)}
              />
            </label>
            <label className="field" htmlFor="end-date">
              <span>Ngày kết thúc <b aria-hidden="true">*</b></span>
              <DateSelect
                value={form.endDate}
                onChange={(v) => onChange("endDate", v)}
              />
            </label>
          </div>
        </fieldset>
        <fieldset>
          <legend>Chi phí theo kỳ</legend>
          <div className="field-grid">
            <Field
              label="Tiền phòng"
              name="rentAmount"
              type="number"
              inputMode="numeric"
              value={form.rentAmount}
              onChange={onChange}
              helper="VND mỗi tháng"
            />
            <Field
              label="Đơn giá điện"
              name="electricityUnitPrice"
              type="number"
              inputMode="numeric"
              value={form.electricityUnitPrice}
              onChange={onChange}
              helper="VND mỗi kWh"
            />
            <Field
              label="Tiền nước cố định"
              name="waterFee"
              type="number"
              inputMode="numeric"
              value={form.waterFee}
              onChange={onChange}
              helper="VND mỗi tháng"
            />
            <Field
              label="Phí dịch vụ cố định"
              name="serviceFee"
              type="number"
              inputMode="numeric"
              value={form.serviceFee}
              onChange={onChange}
              helper="VND mỗi tháng"
            />
          </div>
        </fieldset>
        <fieldset>
          <legend>Ngày đóng tiền</legend>
          <Field
            label="Đóng tiền vào ngày"
            name="paymentDueDay"
            type="number"
            min="1"
            max="28"
            inputMode="numeric"
            value={form.paymentDueDay}
            onChange={onChange}
            helper="Chọn từ ngày 1 đến 28 mỗi tháng"
          />
        </fieldset>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <button className="save-button" type="submit" disabled={isSaving}>
          {isSaving ? "Đang lưu hợp đồng..." : "Lưu hợp đồng"}
        </button>
      </form>
    </section>
  );
}

function Field({
  label,
  name,
  helper,
  onChange,
  ...input
}: {
  label: string;
  name: keyof RentalContractInput;
  helper?: string;
  onChange: (name: keyof RentalContractInput, value: string) => void;
} & Omit<InputHTMLAttributes<HTMLInputElement>, "name" | "onChange">) {
  const id = `contract-${name}`;
  return (
    <label className="field" htmlFor={id}>
      <span>
        {label} <b aria-hidden="true">*</b>
      </span>
      <input
        id={id}
        name={name}
        required
        onChange={(event) => onChange(name, event.target.value)}
        {...input}
      />
      {helper && <small>{helper}</small>}
    </label>
  );
}

function ContractCard({ contract }: { contract: RentalContract }) {
  return (
    <section
      className="contract-panel contract-details"
      aria-labelledby="active-contract-title"
    >
      <div className="panel-heading">
        <div>
          <p className="section-label">Đang hiệu lực</p>
          <h2 id="active-contract-title">Hợp đồng của bạn</h2>
        </div>
        <span className="status-pill">ACTIVE</span>
      </div>
      <div className="contract-period">
        <span>Thời hạn thuê</span>
        <strong>
          {contract.startDate} — {contract.endDate}
        </strong>
      </div>
      <dl className="cost-list">
        <Cost label="Tiền phòng" value={contract.rentAmount} />
        <Cost
          label="Đơn giá điện"
          value={`${money.format(contract.electricityUnitPrice)} / kWh`}
        />
        <Cost label="Nước cố định" value={contract.waterFee} />
        <Cost label="Dịch vụ cố định" value={contract.serviceFee} />
      </dl>
    </section>
  );
}

function MeterReadingForm({
  readings,
  period,
  meterValue,
  error,
  isSaving,
  onPeriodChange,
  onMeterValueChange,
  onSubmit,
}: {
  readings: ElectricityReading[];
  period: string;
  meterValue: string;
  error: string | null;
  isSaving: boolean;
  onPeriodChange: (value: string) => void;
  onMeterValueChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const [selectedYear, setSelectedYear] = useState<string>("");
  const lastReading = readings[0];

  // Lấy 3 tháng gần nhất
  const recentReadings = readings.slice(0, 3);

  // Nhóm theo năm
  const readingsByYear = readings.reduce((acc, reading) => {
    const { year } = parsePeriod(reading.period);
    if (!acc[year]) acc[year] = [];
    acc[year].push(reading);
    return acc;
  }, {} as Record<string, ElectricityReading[]>);

  const years = Object.keys(readingsByYear).sort((a, b) => b.localeCompare(a));

  // Hiển thị: 3 tháng gần nhất hoặc tất cả (có thể lọc theo năm)
  const displayReadings = expanded
    ? selectedYear
      ? readingsByYear[selectedYear] || []
      : readings
    : recentReadings;

  return (
    <section className="contract-panel" aria-labelledby="reading-title">
      <div className="panel-heading">
        <div>
          <p className="section-label">Chỉ số điện</p>
          <h2 id="reading-title">Ghi chỉ số điện</h2>
        </div>
      </div>
      {lastReading && (
        <div className="last-reading-info">
          <span>Kỳ trước ({lastReading.period})</span>
          <strong>{lastReading.meterValue.toLocaleString("vi-VN")} kWh</strong>
        </div>
      )}
      <form onSubmit={onSubmit} noValidate>
        <fieldset>
          <legend>Chỉ số kỳ này</legend>
          <div className="field-grid two-cols">
            <label className="field">
              <span>Kỳ <b aria-hidden="true">*</b></span>
              <MonthSelect
                value={period}
                onChange={onPeriodChange}
              />
            </label>
            <label className="field" htmlFor="reading-meter">
              <span>Chỉ số mới <b aria-hidden="true">*</b></span>
              <input
                id="reading-meter"
                type="number"
                inputMode="numeric"
                min="0"
                value={meterValue}
                onChange={(e) => onMeterValueChange(e.target.value)}
                required
                placeholder="kWh"
              />
              <small>kWh</small>
            </label>
          </div>
        </fieldset>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <button className="save-button" type="submit" disabled={isSaving}>
          {isSaving ? "Đang ghi..." : "Ghi chỉ số"}
        </button>
      </form>
      {readings.length > 0 && (
        <div className="readings-list">
          <div className="list-header">
            <h3>Lịch sử chỉ số</h3>
            <button
              type="button"
              className="toggle-button"
              onClick={() => {
                setExpanded(!expanded);
                if (expanded) setSelectedYear("");
              }}
            >
              {expanded ? "▲ Thu gọn" : "▼ Xem tất cả"}
            </button>
          </div>
          {expanded && years.length > 1 && (
            <div className="year-filter">
              <select
                value={selectedYear}
                onChange={(e) => setSelectedYear(e.target.value)}
              >
                <option value="">Tất cả các năm</option>
                {years.map((y) => (
                  <option key={y} value={y}>{y}</option>
                ))}
              </select>
            </div>
          )}
          <ul>
            {displayReadings.map((r) => (
              <li key={r.id}>
                <span>{r.period}</span>
                <strong>{r.meterValue.toLocaleString("vi-VN")} kWh</strong>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}

function BillForm({
  bills,
  period,
  error,
  isSaving,
  onPeriodChange,
  onSubmit,
}: {
  bills: Bill[];
  period: string;
  error: string | null;
  isSaving: boolean;
  onPeriodChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const [selectedYear, setSelectedYear] = useState<string>("");

  // Lấy 3 tháng gần nhất
  const recentBills = bills.slice(0, 3);

  // Nhóm theo năm
  const billsByYear = bills.reduce((acc, bill) => {
    const { year } = parsePeriod(bill.period);
    if (!acc[year]) acc[year] = [];
    acc[year].push(bill);
    return acc;
  }, {} as Record<string, Bill[]>);

  const years = Object.keys(billsByYear).sort((a, b) => b.localeCompare(a));

  // Hiển thị: 3 tháng gần nhất hoặc tất cả (có thể lọc theo năm)
  const displayBills = expanded
    ? selectedYear
      ? billsByYear[selectedYear] || []
      : bills
    : recentBills;

  return (
    <section className="contract-panel" aria-labelledby="bill-title">
      <div className="panel-heading">
        <div>
          <p className="section-label">Hóa đơn</p>
          <h2 id="bill-title">Tạo và xem hóa đơn</h2>
        </div>
      </div>
      <form onSubmit={onSubmit} noValidate>
        <fieldset>
          <legend>Tạo hóa đơn mới</legend>
          <label className="field">
            <span>Kỳ <b aria-hidden="true">*</b></span>
            <MonthSelect
              value={period}
              onChange={onPeriodChange}
            />
            <small>Cần ghi chỉ số điện cho kỳ này trước khi tạo hóa đơn</small>
          </label>
        </fieldset>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <button className="save-button" type="submit" disabled={isSaving}>
          {isSaving ? "Đang tạo..." : "Tạo hóa đơn"}
        </button>
      </form>
      {bills.length > 0 && (
        <div className="bills-list">
          <div className="list-header">
            <h3>Danh sách hóa đơn</h3>
            <button
              type="button"
              className="toggle-button"
              onClick={() => {
                setExpanded(!expanded);
                if (expanded) setSelectedYear("");
              }}
            >
              {expanded ? "▲ Thu gọn" : "▼ Xem tất cả"}
            </button>
          </div>
          {expanded && years.length > 1 && (
            <div className="year-filter">
              <select
                value={selectedYear}
                onChange={(e) => setSelectedYear(e.target.value)}
              >
                <option value="">Tất cả các năm</option>
                {years.map((y) => (
                  <option key={y} value={y}>{y}</option>
                ))}
              </select>
            </div>
          )}
          {displayBills.map((bill) => (
            <BillCard key={bill.id} bill={bill} />
          ))}
        </div>
      )}
    </section>
  );
}

function PaymentForm({
  bills,
  payments,
  selectedBillId,
  paidAt,
  note,
  error,
  isSaving,
  onBillSelect,
  onPaidAtChange,
  onNoteChange,
  onSubmit,
}: {
  bills: Bill[];
  payments: Payment[];
  selectedBillId: string | null;
  paidAt: string;
  note: string;
  error: string | null;
  isSaving: boolean;
  onBillSelect: (billId: string | null) => void;
  onPaidAtChange: (value: string) => void;
  onNoteChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const pendingBills = bills.filter((b) => b.status === "PENDING");

  return (
    <section className="contract-panel" aria-labelledby="payment-title">
      <div className="panel-heading">
        <div>
          <p className="section-label">Thanh toán</p>
          <h2 id="payment-title">Xác nhận thanh toán</h2>
        </div>
      </div>
      {pendingBills.length === 0 ? (
        <p className="no-data">Không có hóa đơn nào chờ thanh toán.</p>
      ) : (
        <form onSubmit={onSubmit} noValidate>
          <fieldset>
            <legend>Chọn hóa đơn</legend>
            <label className="field" htmlFor="bill-select">
              <span>Hóa đơn <b aria-hidden="true">*</b></span>
              <select
                id="bill-select"
                value={selectedBillId || ""}
                onChange={(e) => onBillSelect(e.target.value || null)}
                required
              >
                <option value="">-- Chọn hóa đơn --</option>
                {pendingBills.map((bill) => (
                  <option key={bill.id} value={bill.id}>
                    Kỳ {bill.period} - {money.format(bill.totalAmount)}
                  </option>
                ))}
              </select>
            </label>
          </fieldset>
          <fieldset>
            <legend>Thông tin thanh toán</legend>
            <div className="field-grid two-cols">
              <label className="field" htmlFor="paid-at">
                <span>Ngày thanh toán <b aria-hidden="true">*</b></span>
                <input
                  id="paid-at"
                  type="date"
                  value={paidAt}
                  onChange={(e) => onPaidAtChange(e.target.value)}
                  max={new Date().toISOString().split("T")[0]}
                  required
                />
              </label>
              <label className="field" htmlFor="payment-note">
                <span>Ghi chú</span>
                <input
                  id="payment-note"
                  type="text"
                  value={note}
                  onChange={(e) => onNoteChange(e.target.value)}
                  placeholder="Không bắt buộc"
                />
              </label>
            </div>
          </fieldset>
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <button className="save-button" type="submit" disabled={isSaving || !selectedBillId}>
            {isSaving ? "Đang xác nhận..." : "Xác nhận thanh toán"}
          </button>
        </form>
      )}
      {payments.length > 0 && <PaymentHistory payments={payments} />}
    </section>
  );
}

function PaymentHistory({ payments }: { payments: Payment[] }) {
  const [expanded, setExpanded] = useState(false);
  const [receipts, setReceipts] = useState<Record<string, Receipt>>({});
  const [uploadingPaymentId, setUploadingPaymentId] = useState<string | null>(null);
  const [receiptError, setReceiptError] = useState<string | null>(null);
  const recentPayments = payments.slice(0, 3);
  const displayPayments = expanded ? payments : recentPayments;

  async function handleReceiptUpload(paymentId: string, file: File) {
    setReceiptError(null);
    if (file.size > 5 * 1024 * 1024) {
      setReceiptError("Ảnh chứng từ không được vượt quá 5 MB.");
      return;
    }
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setReceiptError("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP.");
      return;
    }

    setUploadingPaymentId(paymentId);
    try {
      const receipt = await uploadReceipt(paymentId, file);
      setReceipts((current) => ({ ...current, [paymentId]: receipt }));
    } catch (requestError: unknown) {
      setReceiptError(
        requestError instanceof Error
          ? requestError.message
          : "Không thể tải chứng từ lên.",
      );
    } finally {
      setUploadingPaymentId(null);
    }
  }

  return (
    <div className="payments-list">
      <div className="list-header">
        <h3>Lịch sử thanh toán</h3>
        {payments.length > 3 && (
          <button
            type="button"
            className="toggle-button"
            onClick={() => setExpanded(!expanded)}
          >
            {expanded ? "▲ Thu gọn" : "▼ Xem tất cả"}
          </button>
        )}
      </div>
      {receiptError && (
        <p className="form-error" role="alert">
          {receiptError}
        </p>
      )}
      <ul>
        {displayPayments.map((p) => {
          const receipt = receipts[p.id];
          return (
            <li key={p.id} className="payment-item">
              <div className="payment-info">
                <span className="payment-period">Kỳ {p.bill.period}</span>
                <span className="payment-date">
                  {new Date(p.paidAt).toLocaleDateString("vi-VN")}
                </span>
                <div className="receipt-actions">
                  <label className="receipt-upload-button" htmlFor={`receipt-${p.id}`}>
                    {uploadingPaymentId === p.id ? "Đang tải..." : receipt ? "Đổi chứng từ" : "Thêm chứng từ"}
                  </label>
                  <input
                    id={`receipt-${p.id}`}
                    className="receipt-file-input"
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    disabled={uploadingPaymentId !== null}
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      event.currentTarget.value = "";
                      if (file) void handleReceiptUpload(p.id, file);
                    }}
                  />
                  {receipt && (
                    <a
                      className="receipt-link"
                      href={getReceiptUrl(receipt.id)}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Mở ảnh
                    </a>
                  )}
                </div>
              </div>
              <div className="payment-amount">
                <strong>{money.format(p.bill.totalAmount)}</strong>
                <span className={`status-pill ${p.onTime ? "status-paid" : "status-overdue"}`}>
                  {p.onTime ? "Đúng hạn" : "Trễ hạn"}
                </span>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function BillCard({ bill }: { bill: Bill }) {
  // Tính hạn thanh toán: ngày 4 của tháng sau kỳ
  const { year, month } = parsePeriod(bill.period);
  const dueDate = `${year}-${month.padStart(2, "0")}-04`;

  return (
    <article className="bill-card">
      <header className="bill-header">
        <span className="bill-period">Kỳ {bill.period}</span>
        <span className={`status-pill status-${bill.status.toLowerCase()}`}>
          {bill.status}
        </span>
      </header>
      <dl className="bill-breakdown">
        <Cost label="Tiền phòng" value={bill.rentAmount} />
        <div>
          <dt>
            Tiền điện ({bill.consumption.toLocaleString("vi-VN")} kWh ×{" "}
            {money.format(bill.electricityUnitPrice)}/kWh)
          </dt>
          <dd>{money.format(bill.electricityAmount)}</dd>
        </div>
        <Cost label="Nước cố định" value={bill.waterFee} />
        <Cost label="Dịch vụ cố định" value={bill.serviceFee} />
      </dl>
      <footer className="bill-footer">
        <span>Tổng cộng</span>
        <strong>{money.format(bill.totalAmount)}</strong>
      </footer>
      <p className="bill-due">
        Hạn thanh toán: <strong>{dueDate}</strong>
      </p>
      <details className="bill-details">
        <summary>Chi tiết chỉ số</summary>
        <dl>
          <div>
            <dt>Chỉ số cũ</dt>
            <dd>{bill.oldMeterValue.toLocaleString("vi-VN")} kWh</dd>
          </div>
          <div>
            <dt>Chỉ số mới</dt>
            <dd>{bill.newMeterValue.toLocaleString("vi-VN")} kWh</dd>
          </div>
          <div>
            <dt>Tiêu thụ</dt>
            <dd>{bill.consumption.toLocaleString("vi-VN")} kWh</dd>
          </div>
        </dl>
      </details>
    </article>
  );
}

function Cost({ label, value }: { label: string; value: number | string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd>{typeof value === "number" ? money.format(value) : value}</dd>
    </div>
  );
}

function LoadingState() {
  return (
    <section
      className="contract-panel loading-card"
      aria-busy="true"
      aria-label="Đang tải hợp đồng"
    >
      <span />
      <span />
      <span />
    </section>
  );
}

export default App;
