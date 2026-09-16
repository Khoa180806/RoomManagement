import { useEffect, useState } from "react";
import type { FormEvent, InputHTMLAttributes } from "react";
import {
  createRentalContract,
  getActiveRentalContract,
  type RentalContract,
  type RentalContractInput,
} from "./features/contracts/api";
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

function App() {
  const [contract, setContract] = useState<RentalContract | null>(null);
  const [form, setForm] = useState(initialForm);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getActiveRentalContract()
      .then(setContract)
      .catch((requestError: Error) => {
        if (requestError.message !== "NOT_FOUND")
          setError("Không thể tải hợp đồng. Hãy thử lại sau.");
      })
      .finally(() => setIsLoading(false));
  }, []);
  function updateField(name: keyof RentalContractInput, value: string) {
    setForm((current) => ({ ...current, [name]: value }));
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setIsSaving(true);
    try {
      setContract(await createRentalContract(form));
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Không thể lưu hợp đồng.",
      );
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <a className="brand" href="#main-content">
          <span className="brand-mark">RM</span>
          <span>Nhà trọ của tôi</span>
        </a>
        <span className="topbar-label">Thiết lập ban đầu</span>
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
          <ContractCard contract={contract} />
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
