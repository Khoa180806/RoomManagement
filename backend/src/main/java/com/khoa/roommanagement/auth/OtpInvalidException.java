package com.khoa.roommanagement.auth;

public class OtpInvalidException extends RuntimeException {

	public OtpInvalidException(String message) {
		super(message);
	}
}
