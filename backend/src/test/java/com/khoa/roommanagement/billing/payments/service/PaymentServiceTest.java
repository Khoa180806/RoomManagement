package com.khoa.roommanagement.billing.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.entity.BillStatus;
import com.khoa.roommanagement.billing.bills.exception.BillNotFoundException;
import com.khoa.roommanagement.billing.bills.repository.BillRepository;
import com.khoa.roommanagement.billing.contracts.dto.CreateRentalContractCommand;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.exception.ContractTerminatedException;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import com.khoa.roommanagement.billing.payments.entity.Payment;
import com.khoa.roommanagement.billing.payments.exception.BillAlreadyPaidException;
import com.khoa.roommanagement.billing.payments.exception.IdempotencyPayloadMismatchException;
import com.khoa.roommanagement.billing.payments.exception.InvalidPaidAtException;
import com.khoa.roommanagement.billing.payments.repository.PaymentRepository;
import com.khoa.roommanagement.common.time.BusinessZone;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private RentalContractRepository contractRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void confirmsPaymentSuccessfully() {
        RentalContract contract = activeContract();
        Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-08");
        UUID billId = bill.getId();
        Instant paidAt = Instant.now().minusSeconds(3600);
        String idempotencyKey = "key-123";

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.confirmPayment(billId, paidAt, "Test note", idempotencyKey);

        assertThat(payment.getBill().getId()).isEqualTo(billId);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        assertThat(payment.getNote()).isEqualTo("Test note");
        assertThat(payment.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(payment.getPayloadHash()).isNotBlank();
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);

        verify(paymentRepository).save(any());
        verify(billRepository).save(bill);
    }

    @Test
    void acceptsOverdueBill() {
        RentalContract contract = activeContract();
        Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-08");
        bill.setStatus(BillStatus.OVERDUE);
        UUID billId = bill.getId();
        Instant paidAt = Instant.now().minusSeconds(3600);

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.confirmPayment(billId, paidAt, null, null);

        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(payment.getBill().getId()).isEqualTo(billId);
    }

    @Test
    void returnsExistingPaymentForSameIdempotencyKeyAndPayload() {
        RentalContract contract = activeContract();
        Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-08");
        UUID billId = bill.getId();
        Instant paidAt = Instant.now().minusSeconds(3600);
        String idempotencyKey = "key-123";

        Payment existingPayment = Payment.confirm(bill, paidAt, "Test note", idempotencyKey);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        Payment payment = paymentService.confirmPayment(billId, paidAt, "Test note", idempotencyKey);

        assertThat(payment.getId()).isEqualTo(existingPayment.getId());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void throwsIdempotencyConflictForDifferentPayload() {
        RentalContract contract = activeContract();
        Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-08");
        UUID billId = bill.getId();
        Instant paidAt = Instant.now().minusSeconds(3600);
        String idempotencyKey = "key-123";

        Payment existingPayment = Payment.confirm(bill, paidAt, "Test note", idempotencyKey);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.confirmPayment(billId, paidAt, "Ghi chú khác", idempotencyKey))
            .isInstanceOf(IdempotencyPayloadMismatchException.class);
    }

    @Test
    void throwsIdempotencyConflictForDifferentBill() {
        RentalContract contract = activeContract();
        Bill bill1 = Bill.createFrom(contract, 1200L, 1000L, "2026-08");
        Bill bill2 = Bill.createFrom(contract, 1300L, 1200L, "2026-11");
        UUID billId = bill2.getId();
        Instant paidAt = Instant.now().minusSeconds(3600);
        String idempotencyKey = "key-123";

        Payment existingPayment = Payment.confirm(bill1, paidAt, "Test note", idempotencyKey);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        assertThatThrownBy(() -> paymentService.confirmPayment(billId, paidAt, "Test note", idempotencyKey))
            .isInstanceOf(IdempotencyPayloadMismatchException.class);
    }

    @Test
    void throwsBillAlreadyPaidException() {
        RentalContract contract = activeContract();
        Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-08");
        bill.setStatus(BillStatus.PAID);
        UUID billId = bill.getId();
        Instant paidAt = Instant.now().minusSeconds(3600);

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));

        assertThatThrownBy(() -> paymentService.confirmPayment(billId, paidAt, "Test note", null))
            .isInstanceOf(BillAlreadyPaidException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void throwsContractTerminatedException() {
        RentalContract contract = activeContract();
        contract.setStatus(RentalContractStatus.TERMINATED_FOR_NON_PAYMENT);
        Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-08");
        UUID billId = bill.getId();
        Instant paidAt = Instant.now().minusSeconds(3600);

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> paymentService.confirmPayment(billId, paidAt, "Test note", null))
            .isInstanceOf(ContractTerminatedException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void throwsInvalidPaidAtException() {
        RentalContract contract = activeContract();
        Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-08");
        UUID billId = bill.getId();
        Instant paidAt = Instant.now().plusSeconds(3600);

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> paymentService.confirmPayment(billId, paidAt, "Test note", null))
            .isInstanceOf(InvalidPaidAtException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void calculatesOnTimePayment() {
        RentalContract contract = activeContract();
        Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2024-01");
        UUID billId = bill.getId();

        // Đúng hạn: thanh toán sáng ngày đến hạn (2024-01-05)
        Instant onTimePaidAt = LocalDate.of(2024, 1, 5).atStartOfDay()
            .atZone(BusinessZone.VIETNAM).toInstant();

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment onTimePayment = paymentService.confirmPayment(billId, onTimePaidAt, null, null);
        assertThat(onTimePayment.isOnTime()).isTrue();
    }

    @Test
    void countsWholeDueDayAsOnTime() {
        RentalContract contract = activeContract();
        Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2024-01");
        UUID billId = bill.getId();

        // Tối muộn ngày đến hạn vẫn là đúng hạn theo Asia/Ho_Chi_Minh
        Instant eveningOfDueDay = LocalDate.of(2024, 1, 5).atTime(LocalTime.of(22, 30))
            .atZone(BusinessZone.VIETNAM).toInstant();

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.confirmPayment(billId, eveningOfDueDay, null, null);
        assertThat(payment.isOnTime()).isTrue();
    }

    @Test
    void calculatesLatePayment() {
        RentalContract contract = activeContract();
        Bill bill = Bill.createFrom(contract, 1300L, 1200L, "2024-01");
        UUID billId = bill.getId();

        // Trễ hạn: trưa ngày sau ngày đến hạn
        Instant latePaidAt = LocalDate.of(2024, 1, 6).atTime(LocalTime.NOON)
            .atZone(BusinessZone.VIETNAM).toInstant();

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment latePayment = paymentService.confirmPayment(billId, latePaidAt, null, null);
        assertThat(latePayment.isOnTime()).isFalse();
    }

    @Test
    void throwsBillNotFoundException() {
        UUID billId = UUID.randomUUID();
        Instant paidAt = Instant.now().minusSeconds(3600);

        when(billRepository.findById(billId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(billId, paidAt, "Test note", null))
            .isInstanceOf(BillNotFoundException.class);

        verify(paymentRepository, never()).save(any());
    }

    private RentalContract activeContract() {
        return RentalContract.createActive(
            new CreateRentalContractCommand(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2027, 8, 31),
                5,
                3_500_000L,
                4_000L,
                100_000L,
                150_000L
            )
        );
    }
}
