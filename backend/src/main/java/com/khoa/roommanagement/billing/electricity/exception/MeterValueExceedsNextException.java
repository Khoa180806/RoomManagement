package com.khoa.roommanagement.billing.electricity.exception;

public class MeterValueExceedsNextException extends RuntimeException {

	public MeterValueExceedsNextException(long provided, long next) {
		super("Chỉ số điện " + provided + " cao hơn chỉ số kỳ sau " + next + ".");
	}
}
