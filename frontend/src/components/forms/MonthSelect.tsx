import { VIETNAMESE_MONTHS, getYears, makePeriod, parsePeriod } from "../../shared/lib/date";
import { useDraftParts } from "./useDraftParts";

type MonthSelectProps = {
  value: string;
  onChange: (value: string) => void;
  label?: string;
};

const selectClassName = "min-h-12 flex-1 rounded-[0.35rem] border border-line-strong bg-white px-3 py-2 text-ink focus:border-clay focus:outline-none focus:ring-4 focus:ring-focus";

export function MonthSelect({ value, onChange, label = "Kỳ" }: MonthSelectProps) {
  const [parts, updateDraft] = useDraftParts(value, parsePeriod);
  const years = getYears();

  function selectPart(next: { year: string; month: string }) {
    updateDraft(next);
    if (next.year && next.month) onChange(makePeriod(next.year, next.month));
  }

  return (
    <div className="flex gap-2">
      <select className={selectClassName} value={parts.year} onChange={(event) => selectPart({ year: event.target.value, month: parts.month })} aria-label={`${label} - Năm`}>
        <option value="">Năm</option>
        {years.map((year) => <option key={year} value={year}>{year}</option>)}
      </select>
      <select className={selectClassName} value={parts.month} onChange={(event) => selectPart({ year: parts.year, month: event.target.value })} aria-label={`${label} - Tháng`}>
        <option value="">Tháng</option>
        {VIETNAMESE_MONTHS.map((monthName, index) => <option key={monthName} value={String(index + 1).padStart(2, "0")}>{monthName}</option>)}
      </select>
    </div>
  );
}
