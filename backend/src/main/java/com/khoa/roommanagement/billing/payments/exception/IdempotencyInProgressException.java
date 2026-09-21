package com.khoa.roommanagement.billing.payments.exception;

public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException(String message) {
        super(message);
    }
}
