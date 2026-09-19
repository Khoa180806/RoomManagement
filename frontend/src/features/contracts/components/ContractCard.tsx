import { Cost } from "../../../components/data-display/Cost";
import { StatusPill } from "../../../components/data-display/StatusPill";
import type { RentalContract } from "../api";

export function ContractCard({ contract }: { contract: RentalContract }) {
  return (
    <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8" aria-labelledby="active-contract-title">
      <div className="mb-7 flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Đang hiệu lực</p>
          <h2 id="active-contract-title" className="mt-1 font-display text-[1.55rem] tracking-[-0.025em] text-ink">Hợp đồng của bạn</h2>
        </div>
        <StatusPill status="ACTIVE" />
      </div>
      <div className="grid gap-1 border-b border-line pb-5">
        <span className="text-xs text-muted">Thời hạn thuê</span>
        <strong className="text-[0.95rem] text-ink">{contract.startDate} — {contract.endDate}</strong>
      </div>
      <dl className="my-4 grid">
        <Cost label="Tiền phòng" value={contract.rentAmount} />
        <Cost label="Đơn giá điện" value={`${new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(contract.electricityUnitPrice)} / kWh`} />
        <Cost label="Nước cố định" value={contract.waterFee} />
        <Cost label="Dịch vụ cố định" value={contract.serviceFee} />
      </dl>
    </section>
  );
}
