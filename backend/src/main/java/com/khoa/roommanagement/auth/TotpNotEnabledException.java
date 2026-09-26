package com.khoa.roommanagement.auth;

public class TotpNotEnabledException extends RuntimeException {

	public TotpNotEnabledException() {
		super("Chưa bật TOTP authenticator.");
	}
}
