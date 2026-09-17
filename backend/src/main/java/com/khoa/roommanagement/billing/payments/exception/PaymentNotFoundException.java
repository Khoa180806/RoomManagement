package com.khoa.roommanagement.billing.payments.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID id) {
        super("Không tìm thấy thanh toán " + id + ".");
    }
}
