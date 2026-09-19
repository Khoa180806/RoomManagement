import { useState } from "react";
import { StatusPill } from "../../../components/data-display/StatusPill";
import { formatDate, formatMoney } from "../../../shared/lib/format";
import type { Receipt } from "../../receipts/api";
import { ReceiptUploader } from "../../receipts/components/ReceiptUploader";
import type { Payment } from "../api";

type PaymentHistoryProps = {
  payments: Payment[];
};

export function PaymentHistory({ payments }: PaymentHistoryProps) {
  const [expanded, setExpanded] = useState(false);
  const [receipts, setReceipts] = useState<Record<string, Receipt>>({});
  const displayPayments = expanded ? payments : payments.slice(0, 3);

  return (
    <div className="mt-6 border-t border-line pt-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="m-0 text-[0.95rem] font-bold text-ink">Lịch sử thanh toán</h3>
        {payments.length > 3 && <button type="button" className="rounded-[0.35rem] border border-clay bg-transparent px-3 py-2 text-xs font-semibold text-clay transition hover:bg-clay hover:text-white focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-2" onClick={() => setExpanded((value) => !value)}>{expanded ? "▲ Thu gọn" : "▼ Xem tất cả"}</button>}
      </div>
      <ul className="m-0 list-none p-0">
        {displayPayments.map((payment) => {
          const receipt = receipts[payment.id];
          return (
            <li key={payment.id} className="flex items-center justify-between gap-4 border-b border-line py-3">
              <div className="grid gap-1">
                <span className="text-sm font-semibold text-ink">Kỳ {payment.bill.period}</span>
                <span className="text-xs text-muted">{formatDate(payment.paidAt)}</span>
                <ReceiptUploader paymentId={payment.id} receipt={receipt} onUploaded={(nextReceipt) => setReceipts((current) => ({ ...current, [payment.id]: nextReceipt }))} />
              </div>
              <div className="grid gap-2 text-right">
                <strong className="text-[0.95rem] tabular-nums text-ink">{formatMoney(payment.bill.totalAmount)}</strong>
                <StatusPill status={payment.onTime ? "Đúng hạn" : "Trễ hạn"} />
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
