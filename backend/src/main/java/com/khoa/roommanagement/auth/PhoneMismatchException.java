package com.khoa.roommanagement.auth;

public class PhoneMismatchException extends RuntimeException {

	public PhoneMismatchException() {
		super("Số điện thoại không khớp với chủ nhà.");
	}
}
