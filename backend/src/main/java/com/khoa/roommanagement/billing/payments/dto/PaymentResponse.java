package com.khoa.roommanagement.billing.payments.dto;

import com.khoa.roommanagement.billing.bills.dto.BillResponse;
import com.khoa.roommanagement.billing.payments.entity.Payment;
import com.khoa.roommanagement.billing.payments.entity.Receipt;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    BillResponse bill,
    Instant paidAt,
    String note,
    boolean onTime,
    Instant createdAt,
    ReceiptSummary receipt
) {
    public record ReceiptSummary(UUID id, String contentType, long fileSize) {
    }

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            BillResponse.from(payment.getBill()),
            payment.getPaidAt(),
            payment.getNote(),
            payment.isOnTime(),
            payment.getCreatedAt(),
            receiptSummaryOf(payment.getReceipt())
        );
    }

    private static ReceiptSummary receiptSummaryOf(Receipt receipt) {
        if (receipt == null) {
            return null;
        }
        return new ReceiptSummary(receipt.getId(), receipt.getContentType(), receipt.getFileSize());
    }
}
