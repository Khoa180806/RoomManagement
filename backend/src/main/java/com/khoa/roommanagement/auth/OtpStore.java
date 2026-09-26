package com.khoa.roommanagement.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Lưu OTP trong bộ nhớ (sống theo ứng dụng, một người dùng nên không cần
 * persistence): mã 6 số hiệu lực 5 phút, tối đa 5 lần nhập sai, giới hạn
 * 3 lần gửi / 15 phút cho mỗi SĐT.
 */
@Component
public class OtpStore {

	public static final Duration OTP_TTL = Duration.ofMinutes(5);
	public static final int MAX_SENDS_PER_WINDOW = 3;
	public static final Duration SEND_WINDOW = Duration.ofMinutes(15);
	public static final int MAX_VERIFY_ATTEMPTS = 5;

	public enum VerifyResult {
		SUCCESS, INVALID, EXPIRED, TOO_MANY_ATTEMPTS
	}

	private record OtpEntry(String code, Instant expiresAt, int attempts) {
	}

	private static final SecureRandom RANDOM = new SecureRandom();

	private final Clock clock;
	private final Map<String, OtpEntry> entries = new HashMap<>();
	private final Map<String, Deque<Instant>> sendLog = new HashMap<>();

	public OtpStore(Clock clock) {
		this.clock = clock;
	}

	/** Ghi nhận một lần gửi OTP; vượt giới hạn ném RateLimitedException. */
	public void recordSend(String phoneKey, Instant now) {
		Deque<Instant> sends = sendLog.computeIfAbsent(phoneKey, ignored -> new ArrayDeque<>());
		Instant cutoff = now.minus(SEND_WINDOW);
		sends.removeIf(sent -> sent.isBefore(cutoff));
		if (sends.size() >= MAX_SENDS_PER_WINDOW) {
			throw new RateLimitedException("Bạn đã yêu cầu OTP quá nhiều lần. Thử lại sau 15 phút.");
		}
		sends.addLast(now);
	}

	public String generateCode() {
		return "%06d".formatted(RANDOM.nextInt(1_000_000));
	}

	public void store(String phoneKey, String code) {
		entries.put(phoneKey, new OtpEntry(code, Instant.now(clock).plus(OTP_TTL), 0));
	}

	public void clear(String phoneKey) {
		entries.remove(phoneKey);
	}

	public VerifyResult verify(String phoneKey, String code) {
		OtpEntry entry = entries.get(phoneKey);
		if (entry == null) {
			return VerifyResult.INVALID;
		}
		Instant now = Instant.now(clock);
		if (now.isAfter(entry.expiresAt())) {
			entries.remove(phoneKey);
			return VerifyResult.EXPIRED;
		}
		if (entry.attempts() >= MAX_VERIFY_ATTEMPTS) {
			entries.remove(phoneKey);
			return VerifyResult.TOO_MANY_ATTEMPTS;
		}
		if (!constantTimeEquals(entry.code(), code == null ? "" : code.strip())) {
			entries.put(phoneKey, new OtpEntry(entry.code(), entry.expiresAt(), entry.attempts() + 1));
			return entry.attempts() + 1 >= MAX_VERIFY_ATTEMPTS ? VerifyResult.TOO_MANY_ATTEMPTS : VerifyResult.INVALID;
		}
		entries.remove(phoneKey);
		return VerifyResult.SUCCESS;
	}

	private static boolean constantTimeEquals(String left, String right) {
		return java.security.MessageDigest.isEqual(
			left.getBytes(java.nio.charset.StandardCharsets.UTF_8),
			right.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}
}
