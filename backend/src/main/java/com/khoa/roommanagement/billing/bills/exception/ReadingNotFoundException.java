package com.khoa.roommanagement.billing.bills.exception;

public class ReadingNotFoundException extends RuntimeException {

	public ReadingNotFoundException(String period) {
		super("Chưa có chỉ số điện cho kỳ " + period + ".");
	}
}
