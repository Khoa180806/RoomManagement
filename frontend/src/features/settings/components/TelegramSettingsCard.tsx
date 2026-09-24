import { useState } from "react";
import { EmptyState, ErrorMessage } from "../../../components/feedback/Feedback";
import { sendTestTelegramMessage } from "../api";

type TelegramSettingsCardProps = {
  onSuccess?: () => void;
};

export function TelegramSettingsCard({ onSuccess }: TelegramSettingsCardProps) {
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  async function handleSendTestMessage() {
    setError(null);
    setSent(false);
    setIsSending(true);
    try {
      await sendTestTelegramMessage();
      setSent(true);
      onSuccess?.();
    } catch (requestError: unknown) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Không thể gửi tin nhắn thử.",
      );
    } finally {
      setIsSending(false);
    }
  }

  return (
    <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8" aria-labelledby="telegram-settings-title">
      <div className="mb-6">
        <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Cài đặt</p>
        <h2 id="telegram-settings-title" className="mt-1 font-display text-[1.55rem] tracking-[-0.025em] text-ink">Telegram nhắc hạn</h2>
        <p className="mt-2 text-sm leading-relaxed text-muted">
          Bot token và chat ID lấy từ biến môi trường trên máy chủ. Gửi tin nhắn thử để kiểm chứng cấu hình trước khi bật nhắc hạn.
        </p>
      </div>
      {sent && <p className="mb-4 border-l-4 border-success-border bg-success-bg p-3 text-sm text-success-ink" role="status">Đã gửi tin nhắn thử. Hãy kiểm tra chat Telegram của bạn.</p>}
      {error && <ErrorMessage message={error} />}
      <button
        className="min-h-[3.2rem] w-full rounded-[0.35rem] bg-ink px-5 text-[0.95rem] font-bold text-white transition hover:bg-clay focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3 disabled:cursor-wait disabled:opacity-65"
        type="button"
        disabled={isSending}
        onClick={() => void handleSendTestMessage()}
      >
        {isSending ? "Đang gửi..." : "Gửi tin nhắn thử"}
      </button>
      {!sent && !error && !isSending && (
        <div className="mt-4">
          <EmptyState message="Chưa kiểm chứng cấu hình Telegram." />
        </div>
      )}
    </section>
  );
}
