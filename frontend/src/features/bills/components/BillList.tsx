import { useState } from "react";
import { Pagination } from "../../../components/data-display/Pagination";
import { StatusPill } from "../../../components/data-display/StatusPill";
import { Cost } from "../../../components/data-display/Cost";
import { parsePeriod } from "../../../shared/lib/date";
import { formatMoney } from "../../../shared/lib/format";
import { useYearHistory } from "../../../shared/lib/useYearHistory";
import type { Bill } from "../api";

export function BillCard({ bill }: { bill: Bill }) {
  const [isOpen, setIsOpen] = useState(false);
  const detailsId = `bill-details-${bill.id}`;

  return (
    <article className="mb-4 rounded-[0.35rem] border border-line bg-sand p-4">
      <button
        type="button"
        className="group flex w-full flex-wrap items-center justify-between gap-x-3 gap-y-2 text-left focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3"
        aria-expanded={isOpen}
        aria-controls={detailsId}
        onClick={() => setIsOpen((current) => !current)}
      >
        <span className="grid gap-2">
          <span className="text-[0.95rem] font-bold text-ink">Kỳ {bill.period}</span>
          <span className="text-xs text-clay group-hover:underline">
            {isOpen ? "▲ Thu gọn chi tiết" : "▼ Mở chi tiết"}
          </span>
        </span>
        <span className="grid shrink-0 justify-items-end gap-2">
          <StatusPill status={bill.status} />
          <strong className="text-base tabular-nums text-ink">{formatMoney(bill.totalAmount)}</strong>
        </span>
      </button>

      {isOpen && (
        <div id={detailsId} className="mt-4 border-t border-line pt-3">
          <dl className="m-0">
            <Cost label="Tiền phòng" value={bill.rentAmount} />
            <div className="flex items-center justify-between border-b border-line py-2">
              <dt className="text-xs text-muted">
                Tiền điện ({bill.consumption.toLocaleString("vi-VN")} kWh × {formatMoney(bill.electricityUnitPrice)}/kWh)
              </dt>
              <dd className="m-0 text-sm font-bold tabular-nums text-ink">{formatMoney(bill.electricityAmount)}</dd>
            </div>
            <Cost label="Nước cố định" value={bill.waterFee} />
            <Cost label="Dịch vụ cố định" value={bill.serviceFee} />
          </dl>
          <div className="mt-3 flex items-center justify-between rounded bg-paper p-3">
            <span className="text-sm text-muted">Tổng cộng</span>
            <strong className="text-[1.1rem] tabular-nums text-ink">{formatMoney(bill.totalAmount)}</strong>
          </div>
          <p className="m-0 mt-3 text-right text-xs text-muted">
            Hạn thanh toán: <strong className="text-ink">{bill.dueDate}</strong>
          </p>
          <details className="mt-3 border-t border-line pt-3">
            <summary className="cursor-pointer text-xs font-semibold text-clay">Chi tiết chỉ số</summary>
            <dl className="mt-3 grid gap-2">
              <div className="flex justify-between"><dt className="text-xs text-muted">Chỉ số cũ</dt><dd className="m-0 text-xs tabular-nums text-ink">{bill.oldMeterValue.toLocaleString("vi-VN")} kWh</dd></div>
              <div className="flex justify-between"><dt className="text-xs text-muted">Chỉ số mới</dt><dd className="m-0 text-xs tabular-nums text-ink">{bill.newMeterValue.toLocaleString("vi-VN")} kWh</dd></div>
              <div className="flex justify-between"><dt className="text-xs text-muted">Tiêu thụ</dt><dd className="m-0 text-xs tabular-nums text-ink">{bill.consumption.toLocaleString("vi-VN")} kWh</dd></div>
            </dl>
          </details>
        </div>
      )}
    </article>
  );
}

export function BillList({ bills }: { bills: Bill[] }) {
  const history = useYearHistory(bills, (bill) => parsePeriod(bill.period).year);

  if (bills.length === 0) return null;
  return <div className="mt-6 border-t border-line pt-4">
    <div className="mb-3 flex items-center justify-between gap-3"><h3 className="m-0 text-[0.95rem] font-bold text-ink">Danh sách hóa đơn</h3><button type="button" className="rounded-[0.35rem] border border-clay bg-transparent px-3 py-2 text-xs font-semibold text-clay transition hover:bg-clay hover:text-white focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-2" onClick={history.toggleExpanded}>{history.expanded ? "▲ Thu gọn" : "▼ Xem tất cả"}</button></div>
    {history.expanded && history.years.length > 1 && <label className="mb-4 block"><span className="sr-only">Lọc hóa đơn theo năm</span><select className="min-h-10 w-full rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-sm text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus" value={history.selectedYear} onChange={(event) => history.setSelectedYear(event.target.value)}><option value="">Tất cả các năm</option>{history.years.map((year) => <option key={year} value={year}>{year}</option>)}</select></label>}
    {history.pageItems.map((bill) => <BillCard key={bill.id} bill={bill} />)}
    {history.expanded && history.totalPages > 1 && <Pagination currentPage={history.safePage} totalPages={history.totalPages} onPageChange={history.setCurrentPage} />}
  </div>;
}
