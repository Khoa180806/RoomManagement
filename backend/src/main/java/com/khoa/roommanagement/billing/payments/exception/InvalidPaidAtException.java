package com.khoa.roommanagement.billing.payments.exception;

public class InvalidPaidAtException extends RuntimeException {

    public InvalidPaidAtException() {
        super("Ngày thanh toán không được ở tương lai.");
    }
}
