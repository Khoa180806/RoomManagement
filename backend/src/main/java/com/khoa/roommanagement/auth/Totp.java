package com.khoa.roommanagement.auth;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * TOTP theo RFC 6238: HMAC-SHA1, bước 30 giây, 6 chữ số, dung sai ±1 bước
 * để bù chênh lệch giờ giữa máy người dùng và server.
 */
public final class Totp {

	private static final long STEP_SECONDS = 30;
	private static final int DIGITS = 6;
	private static final int WINDOW = 1;

	private Totp() {
	}

	public static String generateSecret() {
		byte[] secret = new byte[20];
		new java.security.SecureRandom().nextBytes(secret);
		return Base32.encode(secret);
	}

	public static String codeAt(String base32Secret, Instant instant) {
		long counter = instant.getEpochSecond() / STEP_SECONDS;
		return codeFor(Base32.decode(base32Secret), counter);
	}

	public static boolean verify(String base32Secret, String code, Instant now) {
		if (base32Secret == null || code == null) {
			return false;
		}
		String normalized = code.strip();
		if (!normalized.matches("\\d{6}")) {
			return false;
		}
		byte[] key = Base32.decode(base32Secret);
		long currentCounter = now.getEpochSecond() / STEP_SECONDS;
		for (long offset = -WINDOW; offset <= WINDOW; offset++) {
			if (constantTimeEquals(codeFor(key, currentCounter + offset), normalized)) {
				return true;
			}
		}
		return false;
	}

	private static String codeFor(byte[] key, long counter) {
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key, "HmacSHA1"));
			byte[] counterBytes = new byte[8];
			long value = counter;
			for (int i = 7; i >= 0; i--) {
				counterBytes[i] = (byte) value;
				value >>>= 8;
			}
			byte[] hash = mac.doFinal(counterBytes);
			int offset = hash[hash.length - 1] & 0x0F;
			int binary = ((hash[offset] & 0x7F) << 24)
				| ((hash[offset + 1] & 0xFF) << 16)
				| ((hash[offset + 2] & 0xFF) << 8)
				| (hash[offset + 3] & 0xFF);
			return String.format("%0" + DIGITS + "d", binary % 1_000_000);
		} catch (NoSuchAlgorithmException | InvalidKeyException exception) {
			throw new IllegalStateException("Không thể sinh mã TOTP", exception);
		}
	}

	private static boolean constantTimeEquals(String left, String right) {
		return java.security.MessageDigest.isEqual(
			left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
	}
}
