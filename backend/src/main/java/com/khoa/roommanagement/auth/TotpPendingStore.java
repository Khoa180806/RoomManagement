package com.khoa.roommanagement.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Secret TOTP đang chờ xác thực (chưa lưu database cho đến khi người dùng
 * nhập đúng mã lần đầu). Tự hết hạn sau 10 phút.
 */
@Component
public class TotpPendingStore {

	private static final Duration PENDING_TTL = Duration.ofMinutes(10);

	private final Clock clock;
	private final Map<String, PendingSecret> pending = new HashMap<>();

	private record PendingSecret(String secret, Instant expiresAt) {
	}

	public TotpPendingStore(Clock clock) {
		this.clock = clock;
	}

	public void put(String phoneKey, String secret) {
		pending.put(phoneKey, new PendingSecret(secret, Instant.now(clock).plus(PENDING_TTL)));
	}

	public String take(String phoneKey) {
		PendingSecret entry = pending.get(phoneKey);
		if (entry == null) {
			return null;
		}
		pending.remove(phoneKey);
		if (Instant.now(clock).isAfter(entry.expiresAt())) {
			return null;
		}
		return entry.secret();
	}
}
