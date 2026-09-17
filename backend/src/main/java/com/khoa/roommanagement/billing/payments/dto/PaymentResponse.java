package com.khoa.roommanagement.billing.payments.dto;

import com.khoa.roommanagement.billing.bills.dto.BillResponse;
import com.khoa.roommanagement.billing.payments.entity.Payment;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    BillResponse bill,
    Instant paidAt,
    String note,
    boolean onTime,
    Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            BillResponse.from(payment.getBill()),
            payment.getPaidAt(),
            payment.getNote(),
            payment.isOnTime(),
            payment.getCreatedAt()
        );
    }
}
