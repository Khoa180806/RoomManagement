package com.khoa.roommanagement.billing.payments.repository;

import com.khoa.roommanagement.billing.payments.entity.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByBillId(UUID billId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Page<Payment> findAllByOnTime(Boolean onTime, Pageable pageable);
}
