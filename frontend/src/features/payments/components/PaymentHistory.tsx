import { useState } from "react";
import { EmptyState } from "../../../components/feedback/Feedback";
import { Pagination } from "../../../components/data-display/Pagination";
import { StatusPill } from "../../../components/data-display/StatusPill";
import { formatDate, formatMoney } from "../../../shared/lib/format";
import { ReceiptUploader } from "../../receipts/components/ReceiptUploader";
import type { Payment } from "../api";

type PaymentHistoryProps = {
  payments: Payment[];
  currentPage: number;
  totalPages: number;
  onTime: boolean | undefined;
  onPageChange: (page: number) => void;
  onTimeChange: (value: string) => void;
  isLoading: boolean;
};

export function PaymentHistory({
  payments,
  currentPage,
  totalPages,
  onTime,
  onPageChange,
  onTimeChange,
  isLoading,
}: PaymentHistoryProps) {
  const [expanded, setExpanded] = useState(false);
  const [receiptIds, setReceiptIds] = useState<Record<string, string>>({});
  const displayPayments = expanded ? payments : payments.slice(0, 3);

  function handleReceiptUploaded(paymentId: string, receiptId: string) {
    setReceiptIds((current) => ({ ...current, [paymentId]: receiptId }));
  }

  return (
    <div className="mt-6 border-t border-line pt-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="m-0 text-[0.95rem] font-bold text-ink">Lịch sử thanh toán</h3>
        {payments.length > 3 && (
          <button type="button" className="rounded-[0.35rem] border border-clay bg-transparent px-3 py-2 text-xs font-semibold text-clay transition hover:bg-clay hover:text-white focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-2" onClick={() => setExpanded((value) => !value)}>
            {expanded ? "▲ Thu gọn" : "▼ Xem tất cả"}
          </button>
        )}
      </div>
      {expanded && (
        <label className="mb-4 block">
          <span className="sr-only">Lọc thanh toán theo trạng thái</span>
          <select className="min-h-10 w-full rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-sm text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus" value={onTime === undefined ? "" : String(onTime)} onChange={(event) => onTimeChange(event.target.value)}>
            <option value="">Tất cả trạng thái</option>
            <option value="true">Đúng hạn</option>
            <option value="false">Trễ hạn</option>
          </select>
        </label>
      )}
      {isLoading ? <p className="py-6 text-center text-sm text-muted" role="status">Đang tải lịch sử thanh toán...</p> : displayPayments.length === 0 ? <EmptyState message="Không có thanh toán phù hợp." /> : (
        <ul className="m-0 list-none p-0">
          {displayPayments.map((payment) => {
            const receiptId = receiptIds[payment.id] ?? payment.receipt?.id ?? null;
            return (
              <li key={payment.id} className="flex items-center justify-between gap-4 border-b border-line py-3">
                <div className="grid gap-1">
                  <span className="text-sm font-semibold text-ink">Kỳ {payment.bill.period}</span>
                  <span className="text-xs text-muted">{formatDate(payment.paidAt)}</span>
                  <ReceiptUploader paymentId={payment.id} receiptId={receiptId} onUploaded={(nextId) => handleReceiptUploaded(payment.id, nextId)} />
                </div>
                <div className="grid gap-2 text-right">
                  <strong className="text-[0.95rem] tabular-nums text-ink">{formatMoney(payment.bill.totalAmount)}</strong>
                  <StatusPill status={payment.onTime ? "Đúng hạn" : "Trễ hạn"} />
                </div>
              </li>
            );
          })}
        </ul>
      )}
      {expanded && totalPages > 1 && <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={onPageChange} />}
    </div>
  );
}
