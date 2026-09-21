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
import com.khoa.roommanagement.billing.payments.exception.IdempotencyInProgressException;
import com.khoa.roommanagement.billing.payments.exception.IdempotencyPayloadMismatchException;
import com.khoa.roommanagement.billing.payments.exception.InvalidPaidAtException;
import com.khoa.roommanagement.billing.payments.repository.PaymentRepository;
import com.khoa.roommanagement.common.time.BusinessZone;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final RentalContractRepository contractRepository;
    private final ConcurrentHashMap<String, ReentrantLock> idempotencyLocks = new ConcurrentHashMap<>();

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
        String normalizedKey = normalizeKey(idempotencyKey);
        if (normalizedKey == null) {
            return confirmPaymentInternal(billId, paidAt, note, null);
        }

        ReentrantLock lock = idempotencyLocks.computeIfAbsent(normalizedKey, ignored -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new IdempotencyInProgressException("Yêu cầu thanh toán với key này đang được xử lý.");
        }
        try {
            return confirmPaymentInternal(billId, paidAt, note, normalizedKey);
        } finally {
            lock.unlock();
            idempotencyLocks.remove(normalizedKey, lock);
        }
    }

    private Payment confirmPaymentInternal(UUID billId, Instant paidAt, String note, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existingPayment.isPresent()) {
                Payment existing = existingPayment.get();
                String requestHash = Payment.hashPayload(billId, paidAt, note);
                String existingHash = existing.getPayloadHash() == null
                    ? Payment.hashPayload(existing.getBill().getId(), existing.getPaidAt(), existing.getNote())
                    : existing.getPayloadHash();
                if (existing.getBill().getId().equals(billId) && requestHash.equals(existingHash)) {
                    return existing;
                }
                throw new IdempotencyPayloadMismatchException(
                    "Idempotency key đã được sử dụng cho nội dung thanh toán khác.");
            }
        }

        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new com.khoa.roommanagement.billing.bills.exception.BillNotFoundException(billId));

        if (bill.getStatus() == BillStatus.PAID) {
            throw new BillAlreadyPaidException();
        }

        RentalContract contract = contractRepository.findById(bill.getContractId())
            .orElseThrow(RentalContractNotFoundException::new);

        if (contract.getStatus() == RentalContractStatus.TERMINATED_FOR_NON_PAYMENT) {
            throw new ContractTerminatedException();
        }

        if (paidAt.isAfter(Instant.now())) {
            throw new InvalidPaidAtException();
        }

        YearMonth billPeriod = YearMonth.parse(bill.getPeriod());
        LocalDate periodStart = billPeriod.atDay(1);
        Instant periodStartInstant = periodStart.atStartOfDay(BusinessZone.VIETNAM).toInstant();
        if (paidAt.isBefore(periodStartInstant)) {
            throw new InvalidPaidAtException();
        }

        Payment payment = Payment.confirm(bill, paidAt, note, idempotencyKey);
        payment = paymentRepository.save(payment);

        bill.setStatus(BillStatus.PAID);
        billRepository.save(bill);

        return payment;
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        return idempotencyKey.trim();
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
