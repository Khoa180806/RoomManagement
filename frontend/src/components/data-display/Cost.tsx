import { formatMoney } from "../../shared/lib/format";

export function Cost({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="flex items-center justify-between border-b border-line py-3">
      <dt className="text-xs text-muted">{label}</dt>
      <dd className="m-0 text-sm font-bold tabular-nums text-ink">{typeof value === "number" ? formatMoney(value) : value}</dd>
    </div>
  );
}
