package com.khoa.roommanagement.billing.bills.exception;

public class PreviousReadingNotFoundException extends RuntimeException {

	public PreviousReadingNotFoundException(String period) {
		super("Cần chỉ số điện kỳ trước để tạo hóa đơn cho kỳ " + period + ".");
	}
}
