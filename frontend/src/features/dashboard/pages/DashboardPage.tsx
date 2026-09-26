import { LayoutDashboard } from "lucide-react";
import { useWorkspace } from "../../../app/WorkspaceContext";

export function DashboardPage() {
  const { contract, readings, bills } = useWorkspace();

  return (
    <div className="space-y-4">
      <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)]" aria-labelledby="dashboard-title">
        <div className="flex items-center gap-2">
          <LayoutDashboard size={20} className="text-clay" aria-hidden="true" />
          <h1 id="dashboard-title" className="font-display text-2xl tracking-[-0.025em] text-ink">Tổng quan</h1>
        </div>
        <p className="mt-2 text-sm text-muted">
          Theo dõi biến động chi phí thuê phòng của bạn tại đây.
        </p>

        <dl className="mt-5 grid gap-3 sm:grid-cols-3">
          <div className="rounded-[0.35rem] border border-line bg-sand p-4">
            <dt className="text-xs font-semibold text-muted">Hợp đồng</dt>
            <dd className="mt-1 text-sm font-bold text-ink">
              {contract ? `${contract.startDate} → ${contract.endDate}` : "—"}
            </dd>
          </div>
          <div className="rounded-[0.35rem] border border-line bg-sand p-4">
            <dt className="text-xs font-semibold text-muted">Kỳ đã ghi chỉ số</dt>
            <dd className="mt-1 text-sm font-bold tabular-nums text-ink">{readings.length} kỳ</dd>
          </div>
          <div className="rounded-[0.35rem] border border-line bg-sand p-4">
            <dt className="text-xs font-semibold text-muted">Hóa đơn đã phát hành</dt>
            <dd className="mt-1 text-sm font-bold tabular-nums text-ink">{bills.length} hóa đơn</dd>
          </div>
        </dl>

        <div className="mt-5 flex items-start gap-3 rounded-[0.35rem] border border-line border-l-4 border-l-clay bg-paper p-4">
          <span className="inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-clay font-bold text-white" aria-hidden="true">!</span>
          <p className="text-sm leading-relaxed text-muted">
            <strong className="font-bold text-ink">Quy tắc thanh toán:</strong> hạn đóng tiền được cố định
            theo hợp đồng. Trễ quá 3 ngày lịch, hợp đồng sẽ bị hủy.
          </p>
        </div>

        <p className="mt-5 text-xs text-muted" role="note">
          Biểu đồ biến động chi phí và tiêu thụ điện đang được xây dựng — sẽ có ở bản cập nhật kế tiếp.
        </p>
      </section>
    </div>
  );
}
