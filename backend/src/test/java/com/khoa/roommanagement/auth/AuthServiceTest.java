package com.khoa.roommanagement.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khoa.roommanagement.reminders.telegram.TelegramApiClient;
import com.khoa.roommanagement.reminders.telegram.TelegramNotConfiguredException;
import com.khoa.roommanagement.reminders.telegram.TelegramProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");
	private static final String PHONE = "912345678";
	private static final String CHAT_ID = "987654321";
	private static final Pattern OTP_PATTERN = Pattern.compile("\\b\\d{6}\\b");

	@Mock
	private OwnerAccountRepository accountRepository;

	@Mock
	private TelegramApiClient telegramApiClient;

	private OwnerAccount owner;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		owner = OwnerAccount.create(PHONE);
		OwnerAccountService accountService = new OwnerAccountService(accountRepository,
			new AuthProperties("0912 345 678"));
		org.mockito.Mockito.lenient().when(accountRepository.findFirstBy()).thenReturn(Optional.of(owner));
		org.mockito.Mockito.lenient().when(accountRepository.save(any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		authService = new AuthService(
			accountService,
			new OtpStore(Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"))),
			new TotpPendingStore(Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"))),
			telegramApiClient,
			new TelegramProperties("token", CHAT_ID, "https://api.telegram.org"),
			Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh")));
	}

	@Test
	void sendsOtpViaTelegramWhenPhoneMatches() {
		// Người dùng nhập 0912 345 678 — chuẩn hóa về 912345678 khớp chủ nhà
		authService.requestOtp("0912 345 678");

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(telegramApiClient).sendMessage(eq(CHAT_ID), messageCaptor.capture());
		assertThat(messageCaptor.getValue()).contains("Mã đăng nhập").containsPattern("\\b\\d{6}\\b");
	}

	@Test
	void normalizesPhoneWithCountryCode() {
		authService.requestOtp("+84912345678");
		verify(telegramApiClient).sendMessage(anyString(), anyString());
	}

	@Test
	void rejectsPhoneThatDoesNotBelongToOwner() {
		assertThatThrownBy(() -> authService.requestOtp("0987654321"))
			.isInstanceOf(PhoneMismatchException.class);

		verify(telegramApiClient, never()).sendMessage(anyString(), anyString());
	}

	@Test
	void verifiesCorrectOtpAndReturnsOwner() {
		authService.requestOtp(PHONE);
		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(telegramApiClient).sendMessage(anyString(), messageCaptor.capture());
		String otp = sentOtpCode();

		OwnerAccount authenticated = authService.verifyOtp(PHONE, otp);

		assertThat(authenticated.getId()).isEqualTo(owner.getId());
	}

	private String sentOtpCode() {
		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(telegramApiClient).sendMessage(anyString(), messageCaptor.capture());
		Matcher matcher = OTP_PATTERN.matcher(messageCaptor.getValue());
		assertThat(matcher.find()).isTrue();
		return matcher.group();
	}

	@Test
	void rejectsOtpForMismatchedPhone() {
		authService.requestOtp(PHONE);

		assertThatThrownBy(() -> authService.verifyOtp("0987654321", "123456"))
			.isInstanceOf(PhoneMismatchException.class);
	}

	@Test
	void verifiesTotpWhenEnabled() {
		String secret = Totp.generateSecret();
		owner.setTotpSecret(secret);
		String code = Totp.codeAt(secret, NOW);

		OwnerAccount authenticated = authService.verifyTotp(code);

		assertThat(authenticated.getId()).isEqualTo(owner.getId());
	}

	@Test
	void rejectsTotpWhenDisabled() {
		assertThatThrownBy(() -> authService.verifyTotp("123456"))
			.isInstanceOf(TotpNotEnabledException.class);
	}

	@Test
	void totpSetupReturnsUriAndConfirmPersistsSecret() {
		String uri = authService.beginTotpSetup(owner);

		assertThat(uri).startsWith("otpauth://totp/RoomManagement:").contains("secret=");
		String secret = uri.replaceAll(".*secret=([A-Z2-7]+).*", "$1");
		String code = Totp.codeAt(secret, NOW);

		authService.confirmTotpSetup(owner, code);

		assertThat(owner.getTotpSecret()).isEqualTo(secret);
		assertThat(owner.isTotpEnabled()).isTrue();
	}

	@Test
	void totpConfirmFailsWithWrongCode() {
		authService.beginTotpSetup(owner);

		assertThatThrownBy(() -> authService.confirmTotpSetup(owner, "000000"))
			.isInstanceOf(TotpInvalidException.class);
		assertThat(owner.isTotpEnabled()).isFalse();
	}

	@Test
	void disableTotpClearsSecret() {
		owner.setTotpSecret(Totp.generateSecret());

		authService.disableTotp(owner);

		assertThat(owner.isTotpEnabled()).isFalse();
	}

	@Test
	void requestOtpFailsWhenTelegramNotConfigured() {
		AuthService unconfigured = new AuthService(
			new OwnerAccountService(accountRepository, new AuthProperties(PHONE)),
			new OtpStore(Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"))),
			new TotpPendingStore(Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"))),
			telegramApiClient,
			new TelegramProperties("", "", "https://api.telegram.org"),
			Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh")));

		assertThatThrownBy(() -> unconfigured.requestOtp(PHONE))
			.isInstanceOf(TelegramNotConfiguredException.class);
	}

	@Test
	void seedsOwnerAccountFromEnvWhenMissing() {
		when(accountRepository.findFirstBy()).thenReturn(Optional.empty());

		OwnerAccountService seeding = new OwnerAccountService(accountRepository,
			new AuthProperties("+84912345678"));

		Optional<OwnerAccount> created = seeding.getOwner();

		assertThat(created).isPresent();
		assertThat(created.get().getPhone()).isEqualTo("912345678");
	}

	@Test
	void returnsEmptyWhenNotConfigured() {
		when(accountRepository.findFirstBy()).thenReturn(Optional.empty());

		OwnerAccountService unconfigured = new OwnerAccountService(accountRepository, new AuthProperties(""));

		assertThat(unconfigured.getOwner()).isEmpty();
	}

	@Test
	void otpNeverSentToDifferentChatThanConfigured() {
		authService.requestOtp(PHONE);

		verify(telegramApiClient, never()).sendMessage(eq("000000000"), anyString());
	}
}
