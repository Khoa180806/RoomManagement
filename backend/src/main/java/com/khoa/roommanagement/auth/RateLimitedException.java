package com.khoa.roommanagement.auth;

public class RateLimitedException extends RuntimeException {

	public RateLimitedException(String message) {
		super(message);
	}
}
