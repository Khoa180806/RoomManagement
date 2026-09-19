package com.khoa.roommanagement.billing.payments.exception;

public class ReceiptNotFoundException extends RuntimeException {

	public ReceiptNotFoundException() {
		super("Không tìm thấy chứng từ.");
	}
}
