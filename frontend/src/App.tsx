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
import "./App.css";

const initialForm: RentalContractInput = {
  startDate: "",
  endDate: "",
  paymentDueDay: "5",
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

  useEffect(() => {
    getActiveRentalContract()
      .then((c) => {
        setContract(c);
        return Promise.all([getElectricityReadings(), getBills()]);
      })
      .then(([readingsData, billsData]) => {
        setReadings(readingsData);
        setBills(billsData.content);
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

    // Client-side validation
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

    // Client-side validation
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

    // Client-side validation
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
            <Field
              label="Ngày bắt đầu"
              name="startDate"
              type="date"
              value={form.startDate}
              onChange={onChange}
            />
            <Field
              label="Ngày kết thúc"
              name="endDate"
              type="date"
              value={form.endDate}
              onChange={onChange}
            />
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
      <p className="due-note">
        Đóng tiền trước ngày <strong>{contract.paymentDueDay}</strong> mỗi
        tháng.
      </p>
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
  const lastReading = readings[0];
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
            <label className="field" htmlFor="reading-period">
              <span>Kỳ (YYYY-MM) <b aria-hidden="true">*</b></span>
              <input
                id="reading-period"
                type="month"
                value={period}
                onChange={(e) => onPeriodChange(e.target.value)}
                required
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
          <h3>Lịch sử chỉ số</h3>
          <ul>
            {readings.map((r) => (
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
          <label className="field" htmlFor="bill-period">
            <span>Kỳ (YYYY-MM) <b aria-hidden="true">*</b></span>
            <input
              id="bill-period"
              type="month"
              value={period}
              onChange={(e) => onPeriodChange(e.target.value)}
              required
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
          <h3>Danh sách hóa đơn</h3>
          {bills.map((bill) => (
            <BillCard key={bill.id} bill={bill} />
          ))}
        </div>
      )}
    </section>
  );
}

function BillCard({ bill }: { bill: Bill }) {
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
        Hạn thanh toán: <strong>{bill.dueDate}</strong>
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
