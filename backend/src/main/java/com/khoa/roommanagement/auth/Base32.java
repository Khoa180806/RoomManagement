package com.khoa.roommanagement.auth;

/**
 * Mã hóa Base32 (RFC 4648, không padding) dùng cho secret TOTP.
 */
public final class Base32 {

	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

	private Base32() {
	}

	public static String encode(byte[] data) {
		StringBuilder result = new StringBuilder();
		int buffer = 0;
		int bitsLeft = 0;
		for (byte b : data) {
			buffer = (buffer << 8) | (b & 0xFF);
			bitsLeft += 8;
			while (bitsLeft >= 5) {
				result.append(ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
				bitsLeft -= 5;
			}
		}
		if (bitsLeft > 0) {
			result.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
		}
		return result.toString();
	}

	public static byte[] decode(String input) {
		String normalized = input.trim().replace("=", "").toUpperCase();
		byte[] output = new byte[normalized.length() * 5 / 8];
		int buffer = 0;
		int bitsLeft = 0;
		int index = 0;
		for (char c : normalized.toCharArray()) {
			int value = ALPHABET.indexOf(c);
			if (value < 0) {
				throw new IllegalArgumentException("Ký tự Base32 không hợp lệ");
			}
			buffer = (buffer << 5) | value;
			bitsLeft += 5;
			if (bitsLeft >= 8) {
				output[index++] = (byte) (buffer >> (bitsLeft - 8));
				bitsLeft -= 8;
			}
		}
		return output;
	}
}
