package com.khoa.roommanagement.auth;

/**
 * Chuẩn hóa SĐT Việt Nam về 9 số cuối (số quốc gia nội địa) để so khớp
 * bất kể người dùng nhập 09xx, +849xx hay 849xx.
 */
public final class Phone {

	private Phone() {
	}

	public static String normal(String raw) {
		if (raw == null) {
			return "";
		}
		String digits = raw.replaceAll("\\D", "");
		if (digits.length() > 9 && digits.startsWith("84")) {
			digits = digits.substring(2);
		}
		if (digits.length() == 10 && digits.startsWith("0")) {
			digits = digits.substring(1);
		}
		return digits;
	}

	/** 0912345678 → 091****678 để trả về client mà không lộ full. */
	public static String mask(String phone) {
		if (phone == null || phone.length() < 6) {
			return "***";
		}
		return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
	}
}
