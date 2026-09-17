package com.khoa.roommanagement.billing.electricity.exception;

public class NonConsecutivePeriodException extends RuntimeException {

	public NonConsecutivePeriodException(String requestedPeriod, String lastPeriod) {
		super("Kỳ " + requestedPeriod + " không phải là kỳ tiếp theo sau kỳ " + lastPeriod + ".");
	}
}
