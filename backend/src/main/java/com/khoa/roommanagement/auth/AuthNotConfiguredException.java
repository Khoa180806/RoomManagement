package com.khoa.roommanagement.auth;

public class AuthNotConfiguredException extends RuntimeException {

	public AuthNotConfiguredException() {
		super("Chưa cấu hình SĐT chủ nhà. Đặt OWNER_PHONE trong biến môi trường.");
	}
}
