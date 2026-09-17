package com.khoa.roommanagement.billing.payments.service;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.entity.BillStatus;
import com.khoa.roommanagement.billing.bills.repository.BillRepository;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.exception.ContractTerminatedException;
import com.khoa.roommanagement.billing.contracts.exception.RentalContractNotFoundException;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import com.khoa.roommanagement.billing.payments.entity.Payment;
import com.khoa.roommanagement.billing.payments.exception.BillAlreadyPaidException;
import com.khoa.roommanagement.billing.payments.exception.IdempotencyConflictException;
import com.khoa.roommanagement.billing.payments.exception.InvalidPaidAtException;
import com.khoa.roommanagement.billing.payments.repository.PaymentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final RentalContractRepository contractRepository;

    public PaymentService(
        PaymentRepository paymentRepository,
        BillRepository billRepository,
        RentalContractRepository contractRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.billRepository = billRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional
    public Payment confirmPayment(UUID billId, Instant paidAt, String note, String idempotencyKey) {
        // Check idempotency key
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPayment.isPresent()) {
                Payment existing = existingPayment.get();
                if (existing.getBill().getId().equals(billId)) {
                    return existing; // Idempotent return
                } else {
                    throw new IdempotencyConflictException("Idempotency key đã được sử dụng cho hóa đơn khác.");
                }
            }
        }

        // Validate bill
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new com.khoa.roommanagement.billing.bills.exception.BillNotFoundException(billId));

        if (bill.getStatus() != BillStatus.PENDING) {
            throw new BillAlreadyPaidException();
        }

        // Validate contract not terminated
        RentalContract contract = contractRepository.findById(bill.getContractId())
            .orElseThrow(RentalContractNotFoundException::new);

        if (contract.getStatus() == RentalContractStatus.TERMINATED_FOR_NON_PAYMENT) {
            throw new ContractTerminatedException();
        }

        // Validate paidAt not in future
        if (paidAt.isAfter(Instant.now())) {
            throw new InvalidPaidAtException();
        }

        // Create payment
        Payment payment = Payment.confirm(bill, paidAt, note, idempotencyKey);
        payment = paymentRepository.save(payment);

        // Update bill status
        bill.setStatus(BillStatus.PAID);
        billRepository.save(bill);

        return payment;
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPayments(Boolean onTime, Pageable pageable) {
        return paymentRepository.findAllByOnTime(onTime, pageable);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID id) {
        return paymentRepository.findById(id)
            .orElseThrow(() -> new com.khoa.roommanagement.billing.payments.exception.PaymentNotFoundException(id));
    }
}
