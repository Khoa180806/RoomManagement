package com.khoa.roommanagement.billing.payments.exception;

public class BillAlreadyPaidException extends RuntimeException {

    public BillAlreadyPaidException() {
        super("Hóa đơn đã được thanh toán.");
    }
}
