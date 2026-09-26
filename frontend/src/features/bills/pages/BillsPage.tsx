import { MeterReadingForm } from "../components/MeterReadingForm";
import { BillForm } from "../components/BillForm";
import { BillList } from "../components/BillList";
import { useWorkspace } from "../../../app/WorkspaceContext";

export function BillsPage() {
  const workspace = useWorkspace();

  return (
    <div className="space-y-4">
      <MeterReadingForm
        readings={workspace.readings}
        period={workspace.readingPeriod}
        meterValue={workspace.readingMeterValue}
        error={workspace.readingError}
        isSaving={workspace.isRecordingReading}
        onPeriodChange={workspace.setReadingPeriod}
        onMeterValueChange={workspace.setReadingMeterValue}
        onSubmit={workspace.recordReading}
      />

      <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)]" aria-labelledby="bill-title">
        <div className="mb-6">
          <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Hóa đơn</p>
          <h2 id="bill-title" className="mt-1 font-display text-[1.55rem] tracking-[-0.025em] text-ink">Tạo và xem hóa đơn</h2>
        </div>
        <BillForm
          period={workspace.billPeriod}
          error={workspace.billError}
          isSaving={workspace.isCreatingBill}
          onPeriodChange={workspace.setBillPeriod}
          onSubmit={workspace.createNewBill}
        />
        <BillList bills={workspace.bills} />
      </section>
    </div>
  );
}
