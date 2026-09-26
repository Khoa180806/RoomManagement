import { PaymentForm } from "../components/PaymentForm";
import { useWorkspace } from "../../../app/WorkspaceContext";

export function PaymentsPage() {
  const workspace = useWorkspace();

  return (
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
  );
}
