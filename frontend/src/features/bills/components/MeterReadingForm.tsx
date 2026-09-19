import { useState } from "react";
import type { FormEvent } from "react";
import { MonthSelect } from "../../../components/forms/MonthSelect";
import { Pagination } from "../../../components/data-display/Pagination";
import { ErrorMessage } from "../../../components/feedback/Feedback";
import { parsePeriod } from "../../../shared/lib/date";
import type { ElectricityReading } from "../api";

type MeterReadingFormProps = {
  readings: ElectricityReading[];
  period: string;
  meterValue: string;
  error: string | null;
  isSaving: boolean;
  onPeriodChange: (value: string) => void;
  onMeterValueChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

function ReadingHistory({ readings }: { readings: ElectricityReading[] }) {
  const [expanded, setExpanded] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [selectedYear, setSelectedYear] = useState("");
  const recentReadings = readings.slice(0, 3);
  const readingsByYear = readings.reduce<Record<string, ElectricityReading[]>>((groups, reading) => {
    const { year } = parsePeriod(reading.period);
    if (!groups[year]) groups[year] = [];
    groups[year].push(reading);
    return groups;
  }, {});
  const years = Object.keys(readingsByYear).sort((a, b) => b.localeCompare(a));
  const filteredReadings = expanded && selectedYear ? readingsByYear[selectedYear] ?? [] : readings;
  const pageSize = 6;
  const totalPages = Math.max(1, Math.ceil(filteredReadings.length / pageSize));
  const safePage = Math.min(currentPage, totalPages - 1);
  const displayReadings = expanded
    ? filteredReadings.slice(safePage * pageSize, (safePage + 1) * pageSize)
    : recentReadings;

  return (
    <div className="mt-6 border-t border-line pt-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="m-0 text-[0.95rem] font-bold text-ink">Lịch sử chỉ số</h3>
        <button type="button" className="rounded-[0.35rem] border border-clay bg-transparent px-3 py-2 text-xs font-semibold text-clay transition hover:bg-clay hover:text-white focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-2" onClick={() => { setExpanded((value) => !value); setCurrentPage(0); if (expanded) setSelectedYear(""); }}>
          {expanded ? "▲ Thu gọn" : "▼ Xem tất cả"}
        </button>
      </div>
      {expanded && years.length > 1 && (
        <label className="mb-4 block">
          <span className="sr-only">Lọc chỉ số theo năm</span>
          <select className="min-h-10 w-full rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-sm text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus" value={selectedYear} onChange={(event) => { setSelectedYear(event.target.value); setCurrentPage(0); }}>
            <option value="">Tất cả các năm</option>
            {years.map((year) => <option key={year} value={year}>{year}</option>)}
          </select>
        </label>
      )}
      <ul className="m-0 list-none p-0">
        {displayReadings.map((reading) => (
          <li key={reading.id} className="flex items-center justify-between border-b border-line py-3">
            <span className="text-sm text-muted">{reading.period}</span>
            <strong className="text-sm tabular-nums text-ink">{reading.meterValue.toLocaleString("vi-VN")} kWh</strong>
          </li>
        ))}
      </ul>
      {expanded && totalPages > 1 && <Pagination currentPage={safePage} totalPages={totalPages} onPageChange={setCurrentPage} />}
    </div>
  );
}

export function MeterReadingForm({
  readings,
  period,
  meterValue,
  error,
  isSaving,
  onPeriodChange,
  onMeterValueChange,
  onSubmit,
}: MeterReadingFormProps) {
  const lastReading = readings[0];

  return (
    <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8" aria-labelledby="reading-title">
      <div className="mb-7 flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Chỉ số điện</p>
          <h2 id="reading-title" className="mt-1 font-display text-[1.55rem] tracking-[-0.025em] text-ink">Ghi chỉ số điện</h2>
        </div>
      </div>
      {lastReading && <div className="mb-5 flex items-center justify-between gap-3 rounded-[0.35rem] bg-sand px-4 py-3"><span className="text-sm text-muted">Kỳ trước ({lastReading.period})</span><strong className="text-base tabular-nums text-ink">{lastReading.meterValue.toLocaleString("vi-VN")} kWh</strong></div>}
      <form onSubmit={onSubmit} noValidate>
        <fieldset className="mb-6 border-0 p-0">
          <legend className="mb-3 text-sm font-bold text-ink">Chỉ số kỳ này</legend>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="grid gap-2 text-sm font-semibold text-ink"><span>Kỳ <b className="text-clay" aria-hidden="true">*</b></span><MonthSelect label="Kỳ ghi chỉ số" value={period} onChange={onPeriodChange} /></label>
            <label className="grid gap-2 text-sm font-semibold text-ink" htmlFor="reading-meter"><span>Chỉ số mới <b className="text-clay" aria-hidden="true">*</b></span><input id="reading-meter" className="min-h-12 rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus" type="number" inputMode="numeric" min="0" value={meterValue} onChange={(event) => onMeterValueChange(event.target.value)} required placeholder="kWh" /><small className="text-xs font-normal text-muted">kWh</small></label>
          </div>
        </fieldset>
        {error && <ErrorMessage message={error} />}
        <button className="min-h-[3.2rem] w-full rounded-[0.35rem] bg-ink px-5 text-[0.95rem] font-bold text-white transition hover:bg-clay focus-visible:outline-3 focus-visible:outline-clay focus-visible:outline-offset-3 disabled:cursor-wait disabled:opacity-65" type="submit" disabled={isSaving}>{isSaving ? "Đang ghi..." : "Ghi chỉ số"}</button>
      </form>
      {readings.length > 0 && <ReadingHistory readings={readings} />}
    </section>
  );
}
