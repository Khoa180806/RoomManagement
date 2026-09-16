package com.khoa.roommanagement.billing.bills.exception;

public class DuplicateBillException extends RuntimeException {

	public DuplicateBillException(String period) {
		super("Đã có hóa đơn cho kỳ " + period + ".");
	}
}
