import { useState } from "react";
import { VIETNAMESE_MONTHS, getDays, getYears, makeDate, parseDate } from "../../shared/lib/date";

type DateSelectProps = {
  value: string;
  onChange: (value: string) => void;
  label: string;
};

const selectClassName = "min-h-12 flex-1 rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus";

export function DateSelect({ value, onChange, label }: DateSelectProps) {
  const [draft, setDraft] = useState(() => parseDate(value));
  const [draftSource, setDraftSource] = useState(value);
  const parts = draftSource === value ? draft : parseDate(value);
  const years = getYears();
  const days = getDays();

  function updateDraft(next: { year: string; month: string; day: string }) {
    setDraft(next);
    setDraftSource(value);
    if (next.year && next.month && next.day) onChange(makeDate(next.year, next.month, next.day));
  }

  return (
    <div className="flex gap-2">
      <select className={selectClassName} value={parts.day} onChange={(event) => updateDraft({ year: parts.year, month: parts.month, day: event.target.value })} aria-label={`${label} - Ngày`}>
        <option value="">Ngày</option>
        {days.map((day) => <option key={day} value={day.padStart(2, "0")}>{day}</option>)}
      </select>
      <select className={selectClassName} value={parts.month} onChange={(event) => updateDraft({ year: parts.year, month: event.target.value, day: parts.day })} aria-label={`${label} - Tháng`}>
        <option value="">Tháng</option>
        {VIETNAMESE_MONTHS.map((monthName, index) => <option key={monthName} value={String(index + 1).padStart(2, "0")}>{monthName}</option>)}
      </select>
      <select className={selectClassName} value={parts.year} onChange={(event) => updateDraft({ year: event.target.value, month: parts.month, day: parts.day })} aria-label={`${label} - Năm`}>
        <option value="">Năm</option>
        {years.map((year) => <option key={year} value={year}>{year}</option>)}
      </select>
    </div>
  );
}
