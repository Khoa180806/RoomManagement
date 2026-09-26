package com.khoa.roommanagement.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TotpTest {

	// Vector chuẩn RFC 6238: secret = "12345678901234567890"
	private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

	@Test
	void generatesValidBase32Secret() {
		String secret = Totp.generateSecret();

		assertThat(secret).hasSize(32).matches("[A-Z2-7]+");
		byte[] decoded = Base32.decode(secret);
		assertThat(decoded).hasSize(20);
	}

	@Test
	void matchesRfc6238TestVector() {
		// T=59 → counter=1 → 8 chữ số 94287082 → 6 chữ số cuối 287082
		assertThat(Totp.codeAt(RFC_SECRET, Instant.ofEpochSecond(59))).isEqualTo("287082");
		assertThat(Totp.verify(RFC_SECRET, "287082", Instant.ofEpochSecond(59))).isTrue();
	}

	@Test
	void acceptsCodeFromAdjacentWindow() {
		// T=89 → counter=2; mã của counter=1 vẫn hợp lệ nhờ dung sai ±1 bước
		assertThat(Totp.verify(RFC_SECRET, "287082", Instant.ofEpochSecond(89))).isTrue();
	}

	@Test
	void rejectsWrongOrMalformedCode() {
		assertThat(Totp.verify(RFC_SECRET, "000000", Instant.ofEpochSecond(59))).isFalse();
		assertThat(Totp.verify(RFC_SECRET, "28708", Instant.ofEpochSecond(59))).isFalse();
		assertThat(Totp.verify(RFC_SECRET, "abcdef", Instant.ofEpochSecond(59))).isFalse();
		assertThat(Totp.verify(RFC_SECRET, null, Instant.ofEpochSecond(59))).isFalse();
		assertThat(Totp.verify(null, "287082", Instant.ofEpochSecond(59))).isFalse();
	}

	@Test
	void rejectsCodeForDifferentSecret() {
		String otherSecret = Totp.generateSecret();
		assertThat(Totp.verify(otherSecret, Totp.codeAt(RFC_SECRET, Instant.ofEpochSecond(59)),
			Instant.ofEpochSecond(59))).isFalse();
	}

	@Test
	void roundTripsBase32() {
		byte[] original = new byte[]{0, 1, 2, (byte) 250, (byte) 255, 7, 8, 9, 10, 11};
		assertThat(Base32.decode(Base32.encode(original))).isEqualTo(original);
	}
}
