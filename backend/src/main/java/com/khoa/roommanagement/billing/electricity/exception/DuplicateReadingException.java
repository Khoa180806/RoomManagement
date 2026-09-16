package com.khoa.roommanagement.billing.electricity.exception;

public class DuplicateReadingException extends RuntimeException {

	public DuplicateReadingException(String period) {
		super("Đã có chỉ số điện cho kỳ " + period + ".");
	}
}
