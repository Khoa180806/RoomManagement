import { ContractCard } from "../features/contracts/components/ContractCard";
import { ContractForm } from "../features/contracts/components/ContractForm";
import { BillForm } from "../features/bills/components/BillForm";
import { BillList } from "../features/bills/components/BillList";
import { MeterReadingForm } from "../features/bills/components/MeterReadingForm";
import { PaymentForm } from "../features/payments/components/PaymentForm";
import { TelegramSettingsCard } from "../features/settings/components/TelegramSettingsCard";
import { ReminderSettingsCard } from "../features/settings/components/ReminderSettingsCard";
import { ReminderHistory } from "../features/reminders/components/ReminderHistory";
import { LoadingState } from "../components/feedback/Feedback";
import { useRentalWorkspace } from "./useRentalWorkspace";

function App() {
  const workspace = useRentalWorkspace();

  return (
    <main className="min-h-dvh">
      <header className="mx-auto flex max-w-6xl items-center justify-between border-b border-line px-6 py-4">
        <a className="flex items-center gap-2.5 text-sm font-bold text-ink no-underline" href="#main-content">
          <span className="rounded-[0.4rem] bg-ink p-[0.33rem] font-display text-[0.7rem] tracking-[0.05em] text-white">RM</span>
          <span>Nhà trọ của tôi</span>
        </a>
        <span className="text-[0.8125rem] text-muted">Quản lý phòng trọ</span>
      </header>

      <section className="border-b border-line bg-sand px-6 py-14 md:px-8 md:py-20" aria-labelledby="page-title">
        <div className="mx-auto max-w-6xl">
          <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Hợp đồng thuê</p>
          <h1 id="page-title" className="mt-2 mb-5 max-w-3xl font-display text-[clamp(2.45rem,8vw,5rem)] leading-[0.96] tracking-[-0.045em] text-ink">
            Biết rõ từng khoản,
            <br />
            an tâm mỗi kỳ.
          </h1>
          <p className="max-w-xl text-[1.05rem] leading-relaxed text-muted">Lưu cấu hình một lần để tiền phòng, điện, nước và dịch vụ luôn được tính đúng.</p>
        </div>
      </section>

      <div id="main-content" className="mx-auto grid max-w-6xl gap-4 p-6 md:grid-cols-[minmax(0,2fr)_minmax(16rem,1fr)] md:p-8" tabIndex={-1}>
        {workspace.isLoading ? (
          <LoadingState />
        ) : workspace.contract ? (
          <>
            <ContractCard contract={workspace.contract} />
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
            <section className="border border-line bg-paper p-6 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:p-8" aria-labelledby="bill-title">
              <div className="mb-7 flex items-start justify-between gap-4">
                <div>
                  <p className="text-xs font-extrabold uppercase tracking-[0.1em] text-clay">Hóa đơn</p>
                  <h2 id="bill-title" className="mt-1 font-display text-[1.55rem] tracking-[-0.025em] text-ink">Tạo và xem hóa đơn</h2>
                </div>
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
            <PaymentForm
              bills={workspace.bills}
              payments={workspace.payments}
              paymentPage={workspace.paymentPage}
              paymentTotalPages={workspace.paymentTotalPages}
              paymentOnTime={workspace.paymentOnTime}
              onPaymentPageChange={workspace.setPaymentPage}
              onPaymentFilterChange={workspace.setPaymentFilter}
              isLoadingPayments={workspace.isLoadingPayments}
              selectedBillId={workspace.selectedBillId}
              paidAt={workspace.paidAt}
              note={workspace.paymentNote}
              error={workspace.paymentError}
              isSaving={workspace.isConfirmingPayment}
              onBillSelect={workspace.setSelectedBillId}
              onPaidAtChange={workspace.setPaidAt}
              onNoteChange={workspace.setPaymentNote}
              onSubmit={workspace.confirmPaymentHandler}
            />
            <TelegramSettingsCard />
            <ReminderSettingsCard />
            <ReminderHistory />
          </>
        ) : (
          <ContractForm
            form={workspace.form}
            error={workspace.error}
            isSaving={workspace.isSaving}
            onSubmit={workspace.submitContract}
            onChange={workspace.updateField}
          />
        )}

        <aside className="flex items-start gap-3 border border-line border-l-4 border-l-clay bg-paper p-5 shadow-[0_8px_20px_rgba(48,41,30,0.04)] md:col-start-2 md:row-start-1" aria-labelledby="rule-title">
          <span className="inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-clay font-bold text-white" aria-hidden="true">!</span>
          <div>
            <h2 id="rule-title" className="font-body text-[0.95rem] font-bold text-ink">Quy tắc thanh toán</h2>
            <p className="mt-2 text-sm leading-relaxed text-muted">Hạn đóng tiền được cố định theo hợp đồng. Trễ quá 3 ngày lịch, hợp đồng sẽ bị hủy.</p>
          </div>
        </aside>
      </div>
    </main>
  );
}

export default App;
