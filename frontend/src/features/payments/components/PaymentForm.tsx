import type { FormEvent } from "react";
import { ErrorMessage, EmptyState } from "../../../components/feedback/Feedback";
import { formatMoney } from "../../../shared/lib/format";
import type { Bill } from "../../bills/api";
import type { Payment } from "../api";
import { PaymentHistory } from "./PaymentHistory";

type PaymentFormProps = {
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
};

export function PaymentForm({
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
}: PaymentFormProps) {
  const pendingBills = bills.filter((bill) => bill.status === "PENDING");
  const maxDate = new Date().toISOString().split("T")[0];

  return (
    <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8" aria-labelledby="payment-title">
      <div className="mb-7 flex items-start justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Thanh toán</p><h2 id="payment-title" className="mt-1 font-display text-[1.55rem] tracking-[-0.025em] text-ink">Xác nhận thanh toán</h2></div></div>
      {pendingBills.length === 0 ? <EmptyState message="Không có hóa đơn nào chờ thanh toán." /> : <form onSubmit={onSubmit} noValidate>
        <fieldset className="mb-6 border-0 p-0"><legend className="mb-3 text-sm font-bold text-ink">Chọn hóa đơn</legend><label className="grid gap-2 text-sm font-semibold text-ink" htmlFor="bill-select"><span>Hóa đơn <b className="text-clay" aria-hidden="true">*</b></span><select id="bill-select" className="min-h-12 w-full rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus" value={selectedBillId ?? ""} onChange={(event) => onBillSelect(event.target.value || null)} required><option value="">-- Chọn hóa đơn --</option>{pendingBills.map((bill) => <option key={bill.id} value={bill.id}>Kỳ {bill.period} - {formatMoney(bill.totalAmount)}</option>)}</select></label></fieldset>
        <fieldset className="mb-6 border-0 p-0"><legend className="mb-3 text-sm font-bold text-ink">Thông tin thanh toán</legend><div className="grid gap-4 md:grid-cols-2"><label className="grid gap-2 text-sm font-semibold text-ink" htmlFor="paid-at"><span>Ngày thanh toán <b className="text-clay" aria-hidden="true">*</b></span><input id="paid-at" className="min-h-12 rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus" type="date" value={paidAt} onChange={(event) => onPaidAtChange(event.target.value)} max={maxDate} required /></label><label className="grid gap-2 text-sm font-semibold text-ink" htmlFor="payment-note"><span>Ghi chú</span><input id="payment-note" className="min-h-12 rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus" type="text" value={note} onChange={(event) => onNoteChange(event.target.value)} placeholder="Không bắt buộc" /></label></div></fieldset>
        {error && <ErrorMessage message={error} />}
        <button className="min-h-[3.2rem] w-full rounded-[0.35rem] bg-ink px-5 text-[0.95rem] font-bold text-white transition hover:bg-clay focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3 disabled:cursor-wait disabled:opacity-65" type="submit" disabled={isSaving || !selectedBillId}>{isSaving ? "Đang xác nhận..." : "Xác nhận thanh toán"}</button>
      </form>}
      {payments.length > 0 && <PaymentHistory payments={payments} />}
    </section>
  );
}
