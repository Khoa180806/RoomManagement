import { useEffect, useState } from "react";
import { ErrorMessage } from "../../../components/feedback/Feedback";
import { getReminderSettings, updateReminderSettings, type ReminderSettings } from "../api";

type DayToggleProps = {
  label: string;
  allDays: number[];
  selectedDays: number[];
  onChange: (days: number[]) => void;
};

function DayToggle({ label, allDays, selectedDays, onChange }: DayToggleProps) {
  return (
    <fieldset className="m-0 border-0 p-0">
      <legend className="mb-2 text-xs font-bold text-ink">{label}</legend>
      <div className="flex flex-wrap gap-2">
        {allDays.map((day) => {
          const isSelected = selectedDays.includes(day);
          return (
            <button
              key={day}
              type="button"
              aria-pressed={isSelected}
              className={`min-h-9 rounded-[0.35rem] border px-3 text-xs font-semibold transition focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-2 ${isSelected ? "border-clay bg-clay text-white" : "border-line-strong bg-white text-ink hover:border-clay"}`}
              onClick={() =>
                onChange(
                  isSelected ? selectedDays.filter((value) => value !== day) : [...selectedDays, day].sort((a, b) => a - b),
                )
              }
            >
              {day} ngày
            </button>
          );
        })}
      </div>
    </fieldset>
  );
}

type SwitchProps = {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
};

function Switch({ label, checked, onChange }: SwitchProps) {
  return (
    <label className="flex items-center justify-between gap-3 border-b border-line py-2">
      <span className="text-sm font-semibold text-ink">{label}</span>
      <input aria-label={label} type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} className="h-5 w-5 accent-[var(--clay)]" />
    </label>
  );
}

const UPCOMING_OPTIONS = [1, 3, 5, 7];
const OVERDUE_OPTIONS = [1, 2, 3];
const CONTRACT_OPTIONS = [30, 60, 90];

export function ReminderSettingsCard() {
  const [settings, setSettings] = useState<ReminderSettings | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    getReminderSettings()
      .then(setSettings)
      .catch((requestError: unknown) => setError(requestError instanceof Error ? requestError.message : "Không thể tải cấu hình nhắc."))
      .finally(() => setIsLoading(false));
  }, []);

  async function handleSave() {
    if (!settings) return;
    setError(null);
    setSaved(false);
    setIsSaving(true);
    try {
      const { updatedAt: _updatedAt, ...payload } = settings;
      setSettings(await updateReminderSettings(payload));
      setSaved(true);
    } catch (requestError: unknown) {
      setError(requestError instanceof Error ? requestError.message : "Không thể lưu cấu hình nhắc.");
    } finally {
      setIsSaving(false);
    }
  }

  if (isLoading) {
    return (
      <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8" aria-busy="true" aria-label="Đang tải cấu hình nhắc" role="status">
        <span className="block h-10 animate-pulse bg-line" />
      </section>
    );
  }

  if (!settings) {
    return (
      <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8">
        <ErrorMessage message={error ?? "Không thể tải cấu hình nhắc."} />
      </section>
    );
  }

  return (
    <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8" aria-labelledby="reminder-settings-title">
      <div className="mb-4">
        <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Cài đặt</p>
        <h2 id="reminder-settings-title" className="mt-1 font-display text-[1.55rem] tracking-[-0.025em] text-ink">Cấu hình nhắc hạn</h2>
        <p className="mt-2 text-sm leading-relaxed text-muted">Bật/tắt từng loại nhắc và chọn số ngày so với hạn. Job chạy lúc 09:00 mỗi ngày theo giờ Việt Nam.</p>
      </div>
      <div className="grid gap-5">
        <Switch label="Nhắc trước hạn thanh toán" checked={settings.billRemindersEnabled} onChange={(checked) => setSettings({ ...settings, billRemindersEnabled: checked })} />
        {settings.billRemindersEnabled && <DayToggle label="Gửi trước hạn" allDays={UPCOMING_OPTIONS} selectedDays={settings.billReminderDaysBefore} onChange={(days) => setSettings({ ...settings, billReminderDaysBefore: days })} />}
        <Switch label="Nhắc quá hạn (ngày trễ 1–3)" checked={settings.overdueRemindersEnabled} onChange={(checked) => setSettings({ ...settings, overdueRemindersEnabled: checked })} />
        {settings.overdueRemindersEnabled && <DayToggle label="Gửi ở ngày trễ thứ" allDays={OVERDUE_OPTIONS} selectedDays={settings.overdueReminderDays} onChange={(days) => setSettings({ ...settings, overdueReminderDays: days })} />}
        <Switch label="Nhắc hợp đồng sắp hết hạn" checked={settings.contractRemindersEnabled} onChange={(checked) => setSettings({ ...settings, contractRemindersEnabled: checked })} />
        {settings.contractRemindersEnabled && <DayToggle label="Gửi trước ngày hết hạn" allDays={CONTRACT_OPTIONS} selectedDays={settings.contractReminderDaysBefore} onChange={(days) => setSettings({ ...settings, contractReminderDaysBefore: days })} />}
      </div>
      {saved && <p className="mt-4 border-l-4 border-success-border bg-success-bg p-3 text-sm text-success-ink" role="status">Đã lưu cấu hình nhắc.</p>}
      {error && <div className="mt-4"><ErrorMessage message={error} /></div>}
      <button className="mt-5 min-h-[3.2rem] w-full rounded-[0.35rem] bg-ink px-5 text-[0.95rem] font-bold text-white transition hover:bg-clay focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3 disabled:cursor-wait disabled:opacity-65" type="button" disabled={isSaving} onClick={() => void handleSave()}>
        {isSaving ? "Đang lưu..." : "Lưu cấu hình"}
      </button>
    </section>
  );
}
