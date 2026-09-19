export function LoadingState() {
  return (
    <section className="grid gap-4 border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)]" aria-busy="true" aria-label="Đang tải hợp đồng" role="status">
      <span className="block h-10 animate-pulse bg-line" />
      <span className="block h-10 w-4/5 animate-pulse bg-line" />
      <span className="block h-10 w-3/5 animate-pulse bg-line" />
    </section>
  );
}
