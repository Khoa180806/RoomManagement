package com.khoa.roommanagement.billing.payments.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Instant;

public record ConfirmPaymentRequest(
    @NotNull(message = "Ngày thanh toán là bắt buộc")
    @PastOrPresent(message = "Ngày thanh toán không được ở tương lai")
    Instant paidAt,

    String note
) {
}
