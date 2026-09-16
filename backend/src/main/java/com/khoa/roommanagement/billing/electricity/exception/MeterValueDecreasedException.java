package com.khoa.roommanagement.billing.electricity.exception;

public class MeterValueDecreasedException extends RuntimeException {

	public MeterValueDecreasedException(long provided, long previous) {
		super("Chỉ số điện " + provided + " thấp hơn chỉ số kỳ trước " + previous + ".");
	}
}
