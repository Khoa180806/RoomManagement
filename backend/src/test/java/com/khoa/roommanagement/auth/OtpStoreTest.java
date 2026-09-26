package com.khoa.roommanagement.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtpStoreTest {

	private static final String PHONE = "912345678";
	private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");

	private AtomicReference<Instant> nowRef;
	private OtpStore otpStore;

	@BeforeEach
	void setUp() {
		nowRef = new AtomicReference<>(NOW);
		otpStore = new OtpStore(new Clock() {
			@Override
			public ZoneId getZone() {
				return ZoneId.of("UTC");
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return this;
			}

			@Override
			public Instant instant() {
				return nowRef.get();
			}
		});
	}

	private void advanceTo(Instant instant) {
		nowRef.set(instant);
	}

	@Test
	void limitsSendsToThreePerWindow() {
		otpStore.recordSend(PHONE, NOW);
		otpStore.recordSend(PHONE, NOW);
		otpStore.recordSend(PHONE, NOW);

		assertThatThrownBy(() -> otpStore.recordSend(PHONE, NOW))
			.isInstanceOf(RateLimitedException.class);
	}

	@Test
	void allowsSendAgainAfterWindowPasses() {
		otpStore.recordSend(PHONE, NOW);
		otpStore.recordSend(PHONE, NOW);
		otpStore.recordSend(PHONE, NOW);

		advanceTo(NOW.plus(OtpStore.SEND_WINDOW).plusSeconds(1));
		otpStore.recordSend(PHONE, nowRef.get());
		assertThat(otpStore.verify(PHONE, otpStore.generateCode())).isNotEqualTo(OtpStore.VerifyResult.SUCCESS);
	}

	@Test
	void verifiesCorrectCodeAndConsumesIt() {
		otpStore.recordSend(PHONE, NOW);
		String code = otpStore.generateCode();
		otpStore.store(PHONE, code);

		assertThat(otpStore.verify(PHONE, code)).isEqualTo(OtpStore.VerifyResult.SUCCESS);
		// Mã đã dùng bị xóa — không dùng lại được
		assertThat(otpStore.verify(PHONE, code)).isEqualTo(OtpStore.VerifyResult.INVALID);
	}

	@Test
	void rejectsWrongCodeAndCountsAttempts() {
		otpStore.recordSend(PHONE, NOW);
		String code = otpStore.generateCode();
		otpStore.store(PHONE, code);

		assertThat(otpStore.verify(PHONE, "000000")).isEqualTo(OtpStore.VerifyResult.INVALID);
		assertThat(otpStore.verify(PHONE, "000001")).isEqualTo(OtpStore.VerifyResult.INVALID);
		assertThat(otpStore.verify(PHONE, code)).isEqualTo(OtpStore.VerifyResult.SUCCESS);
	}

	@Test
	void locksAfterFiveFailedAttempts() {
		otpStore.recordSend(PHONE, NOW);
		String code = otpStore.generateCode();
		otpStore.store(PHONE, code);

		for (int i = 0; i < 4; i++) {
			assertThat(otpStore.verify(PHONE, "00000" + i)).isEqualTo(OtpStore.VerifyResult.INVALID);
		}
		assertThat(otpStore.verify(PHONE, "999999")).isEqualTo(OtpStore.VerifyResult.TOO_MANY_ATTEMPTS);
		// Đã khóa cả mã đúng
		assertThat(otpStore.verify(PHONE, code)).isEqualTo(OtpStore.VerifyResult.TOO_MANY_ATTEMPTS);
	}

	@Test
	void expiresAfterFiveMinutes() {
		otpStore.recordSend(PHONE, NOW);
		String code = otpStore.generateCode();
		otpStore.store(PHONE, code);

		advanceTo(NOW.plus(OtpStore.OTP_TTL).plusSeconds(1));

		assertThat(otpStore.verify(PHONE, code)).isEqualTo(OtpStore.VerifyResult.EXPIRED);
	}
}
