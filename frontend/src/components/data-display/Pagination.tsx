export function Pagination({ currentPage, totalPages, onPageChange }: { currentPage: number; totalPages: number; onPageChange: (page: number) => void }) {
  return (
    <nav className="mt-4 flex items-center justify-center gap-3 border-t border-line pt-4 text-xs text-muted" aria-label="Phân trang">
      <button type="button" className="min-h-9 rounded-[0.35rem] border border-line-strong bg-paper px-3 text-ink hover:border-clay hover:text-clay focus-visible:outline-2 focus-visible:outline-clay disabled:cursor-not-allowed disabled:opacity-45" disabled={currentPage === 0} onClick={() => onPageChange(currentPage - 1)}>← Trước</button>
      <span aria-live="polite">Trang {currentPage + 1} / {totalPages}</span>
      <button type="button" className="min-h-9 rounded-[0.35rem] border border-line-strong bg-paper px-3 text-ink hover:border-clay hover:text-clay focus-visible:outline-2 focus-visible:outline-clay disabled:cursor-not-allowed disabled:opacity-45" disabled={currentPage >= totalPages - 1} onClick={() => onPageChange(currentPage + 1)}>Sau →</button>
    </nav>
  );
}
