import type { FormEvent } from "react";
import { DateSelect } from "../../../components/forms/DateSelect";
import { Field } from "../../../components/forms/Field";
import { ErrorMessage } from "../../../components/feedback/Feedback";
import type { RentalContractInput } from "../api";

type ContractFormProps = {
  form: RentalContractInput;
  error: string | null;
  isSaving: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onChange: (name: keyof RentalContractInput, value: string) => void;
};

export function ContractForm({
  form,
  error,
  isSaving,
  onSubmit,
  onChange,
}: ContractFormProps) {
  return (
    <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8" aria-labelledby="form-title">
      <div className="mb-7 flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Bước 1 / 1</p>
          <h2 id="form-title" className="mt-1 font-display text-[1.55rem] tracking-[-0.025em] text-ink">Tạo hợp đồng đầu tiên</h2>
        </div>
        <span className="whitespace-nowrap text-xs text-muted"><b className="text-clay" aria-hidden="true">*</b> Bắt buộc</span>
      </div>
      <form onSubmit={onSubmit} noValidate>
        <fieldset className="mb-6 border-0 p-0">
          <legend className="mb-3 text-sm font-bold text-ink">Thời hạn</legend>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="grid gap-2 text-sm font-semibold text-ink">
              <span>Ngày bắt đầu <b className="text-clay" aria-hidden="true">*</b></span>
              <DateSelect label="Ngày bắt đầu" value={form.startDate} onChange={(value) => onChange("startDate", value)} />
            </label>
            <label className="grid gap-2 text-sm font-semibold text-ink">
              <span>Ngày kết thúc <b className="text-clay" aria-hidden="true">*</b></span>
              <DateSelect label="Ngày kết thúc" value={form.endDate} onChange={(value) => onChange("endDate", value)} />
            </label>
          </div>
        </fieldset>
        <fieldset className="mb-6 border-0 p-0">
          <legend className="mb-3 text-sm font-bold text-ink">Chi phí theo kỳ</legend>
          <div className="grid gap-4 md:grid-cols-2">
            <Field label="Tiền phòng" name="rentAmount" type="number" inputMode="numeric" value={form.rentAmount} onChange={onChange} helper="VND mỗi tháng" />
            <Field label="Đơn giá điện" name="electricityUnitPrice" type="number" inputMode="numeric" value={form.electricityUnitPrice} onChange={onChange} helper="VND mỗi kWh" />
            <Field label="Tiền nước cố định" name="waterFee" type="number" inputMode="numeric" value={form.waterFee} onChange={onChange} helper="VND mỗi tháng" />
            <Field label="Phí dịch vụ cố định" name="serviceFee" type="number" inputMode="numeric" value={form.serviceFee} onChange={onChange} helper="VND mỗi tháng" />
          </div>
        </fieldset>
        <fieldset className="mb-6 border-0 p-0">
          <legend className="mb-3 text-sm font-bold text-ink">Ngày đóng tiền</legend>
          <Field label="Đóng tiền vào ngày" name="paymentDueDay" type="number" min="1" max="28" inputMode="numeric" value={form.paymentDueDay} onChange={onChange} helper="Chọn từ ngày 1 đến 28 mỗi tháng" />
        </fieldset>
        {error && <ErrorMessage message={error} />}
        <button className="min-h-[3.2rem] w-full rounded-[0.35rem] bg-ink px-5 text-[0.95rem] font-bold text-white transition hover:bg-clay focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3 disabled:cursor-wait disabled:opacity-65" type="submit" disabled={isSaving}>
          {isSaving ? "Đang lưu hợp đồng..." : "Lưu hợp đồng"}
        </button>
      </form>
    </section>
  );
}
