package com.khoa.roommanagement.auth;

public class TotpInvalidException extends RuntimeException {

	public TotpInvalidException() {
		super("Mã TOTP không đúng hoặc đã hết hạn.");
	}
}
