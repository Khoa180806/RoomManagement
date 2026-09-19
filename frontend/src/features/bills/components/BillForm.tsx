import type { FormEvent } from "react";
import { MonthSelect } from "../../../components/forms/MonthSelect";
import { ErrorMessage } from "../../../components/feedback/Feedback";

type BillFormProps = {
  period: string;
  error: string | null;
  isSaving: boolean;
  onPeriodChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export function BillForm({ period, error, isSaving, onPeriodChange, onSubmit }: BillFormProps) {
  return (
    <form onSubmit={onSubmit} noValidate>
      <fieldset className="mb-6 border-0 p-0">
        <legend className="mb-3 text-sm font-bold text-ink">Tạo hóa đơn mới</legend>
        <label className="grid gap-2 text-sm font-semibold text-ink">
          <span>Kỳ <b className="text-clay" aria-hidden="true">*</b></span>
          <MonthSelect label="Kỳ hóa đơn" value={period} onChange={onPeriodChange} />
          <small className="text-xs font-normal text-muted">Cần ghi chỉ số điện cho kỳ này trước khi tạo hóa đơn</small>
        </label>
      </fieldset>
      {error && <ErrorMessage message={error} />}
      <button className="min-h-[3.2rem] w-full rounded-[0.35rem] bg-ink px-5 text-[0.95rem] font-bold text-white transition hover:bg-clay focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3 disabled:cursor-wait disabled:opacity-65" type="submit" disabled={isSaving}>{isSaving ? "Đang tạo..." : "Tạo hóa đơn"}</button>
    </form>
  );
}
