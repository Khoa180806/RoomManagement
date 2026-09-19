const classes = {
  PENDING: "border-warning-border bg-warning-bg text-warning-ink",
  PAID: "border-success-border bg-success-bg text-success-ink",
  OVERDUE: "border-danger bg-danger-bg text-danger-ink",
  "Đúng hạn": "border-success-border bg-success-bg text-success-ink",
  "Trễ hạn": "border-danger bg-danger-bg text-danger-ink",
  ACTIVE: "border-success-border bg-success-bg text-success-ink",
} as const;

export function StatusPill({ status }: { status: string }) {
  const className = classes[status as keyof typeof classes] ?? "border-line bg-sand text-muted";
  return <span className={`border px-2 py-1 text-[0.72rem] font-extrabold tracking-wider ${className}`}>{status}</span>;
}
