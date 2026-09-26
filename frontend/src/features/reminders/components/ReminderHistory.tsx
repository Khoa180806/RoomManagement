import { useCallback, useEffect, useState } from "react";
import { EmptyState } from "../../../components/feedback/Feedback";
import { Pagination } from "../../../components/data-display/Pagination";
import { formatDate, formatMoney } from "../../../shared/lib/format";
import { getReminders, type Reminder, type ReminderPage } from "../api";

const TYPE_LABELS: Record<Reminder["reminderType"], string> = {
  BILL_UPCOMING: "Nhắc trước hạn",
  BILL_OVERDUE: "Nhắc quá hạn",
  CONTRACT_EXPIRING: "Hợp đồng sắp hết hạn",
  CONTRACT_TERMINATED: "Hợp đồng đã hủy",
};

export function ReminderHistory() {
  const [reminders, setReminders] = useState<Reminder[]>([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const applyPage = useCallback((data: ReminderPage) => {
    setReminders(data.content);
    setCurrentPage(data.number);
    setTotalPages(Math.max(1, data.totalPages));
  }, []);

  const loadPage = useCallback(
    async (page: number) => {
      setIsLoading(true);
      setError(null);
      try {
        applyPage(await getReminders(page, 6));
      } catch (requestError: unknown) {
        setError(requestError instanceof Error ? requestError.message : "Không thể tải lịch sử nhắc.");
      } finally {
        setIsLoading(false);
      }
    },
    [applyPage],
  );

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        if (!cancelled) applyPage(await getReminders(0, 6));
      } catch (requestError: unknown) {
        if (!cancelled) {
          setError(requestError instanceof Error ? requestError.message : "Không thể tải lịch sử nhắc.");
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [applyPage]);

  return (
    <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8" aria-labelledby="reminder-history-title">
      <div className="mb-6">
        <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Nhắc hạn</p>
        <h2 id="reminder-history-title" className="mt-1 font-display text-[1.55rem] tracking-[-0.025em] text-ink">Lịch sử gửi nhắc</h2>
      </div>
      {isLoading ? <p className="py-6 text-center text-sm text-muted" role="status">Đang tải lịch sử nhắc...</p> : error ? <p className="border-l-4 border-danger bg-danger-bg p-3 text-sm text-danger-ink" role="alert">{error}</p> : reminders.length === 0 ? <EmptyState message="Chưa có nhắc nào được gửi." /> : (
        <ul className="m-0 list-none p-0">
          {reminders.map((reminder) => (
            <li key={reminder.id} className="flex items-center justify-between gap-4 border-b border-line py-3">
              <div className="grid gap-1">
                <span className="text-sm font-semibold text-ink">{TYPE_LABELS[reminder.reminderType] ?? reminder.reminderType}</span>
                <span className="text-xs text-muted">
                  {reminder.billPeriod ? `Kỳ ${reminder.billPeriod} · ` : ""}
                  Dự kiến {formatDate(reminder.targetDate)}
                </span>
                {reminder.errorCode && <span className="text-xs font-semibold text-danger-ink">Lỗi: {reminder.errorCode}</span>}
              </div>
              <div className="grid justify-items-end gap-2">
                <span className={`border px-2 py-1 text-[0.72rem] font-extrabold tracking-wider ${reminder.status === "SENT" ? "border-success-border bg-success-bg text-success-ink" : "border-danger bg-danger-bg text-danger-ink"}`}>
                  {reminder.status === "SENT" ? "Đã gửi" : "Thất bại"}
                </span>
                {reminder.billTotalAmount !== null && <strong className="text-sm tabular-nums text-ink">{formatMoney(reminder.billTotalAmount)}</strong>}
              </div>
            </li>
          ))}
        </ul>
      )}
      {!isLoading && totalPages > 1 && <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={(page) => void loadPage(page)} />}
    </section>
  );
}
