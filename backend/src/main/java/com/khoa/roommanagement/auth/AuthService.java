package com.khoa.roommanagement.auth;

import com.khoa.roommanagement.reminders.telegram.TelegramApiClient;
import com.khoa.roommanagement.reminders.telegram.TelegramNotConfiguredException;
import com.khoa.roommanagement.reminders.telegram.TelegramProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final OwnerAccountService accountService;
	private final OtpStore otpStore;
	private final TotpPendingStore totpPendingStore;
	private final TelegramApiClient telegramApiClient;
	private final TelegramProperties telegramProperties;
	private final Clock clock;

	public AuthService(
		OwnerAccountService accountService,
		OtpStore otpStore,
		TotpPendingStore totpPendingStore,
		TelegramApiClient telegramApiClient,
		TelegramProperties telegramProperties,
		Clock clock
	) {
		this.accountService = accountService;
		this.otpStore = otpStore;
		this.totpPendingStore = totpPendingStore;
		this.telegramApiClient = telegramApiClient;
		this.telegramProperties = telegramProperties;
		this.clock = clock;
	}

	/**
	 * Sinh OTP gửi qua Telegram cho SĐT khớp chủ nhà. Trả về thời gian hết
	 * hạn (giây) để client hiển thị đếm ngược.
	 */
	public OtpIssued requestOtp(String rawPhone) {
		OwnerAccount owner = accountService.requireOwner();
		String normalized = Phone.normal(rawPhone);
		if (!normalized.equals(owner.getPhone())) {
			throw new PhoneMismatchException();
		}

		otpStore.recordSend(normalized, Instant.now(clock));

		if (!telegramProperties.isConfigured()) {
			throw new TelegramNotConfiguredException();
		}
		String code = otpStore.generateCode();
		otpStore.store(normalized, code);
		telegramApiClient.sendMessage(telegramProperties.chatId(),
			"🔐 Mã đăng nhập Room Management: " + code + "\nMã hết hạn sau 5 phút. Không chia sẻ cho ai.");
		return new OtpIssued(OtpStore.OTP_TTL.toSeconds());
	}

	public OwnerAccount verifyOtp(String rawPhone, String code) {
		OwnerAccount owner = accountService.requireOwner();
		String normalized = Phone.normal(rawPhone);
		if (!normalized.equals(owner.getPhone())) {
			throw new PhoneMismatchException();
		}
		switch (otpStore.verify(normalized, code)) {
			case SUCCESS -> {
				return owner;
			}
			case EXPIRED -> throw new OtpInvalidException("Mã OTP đã hết hạn. Hãy yêu cầu mã mới.");
			case TOO_MANY_ATTEMPTS ->
				throw new OtpInvalidException("Bạn đã nhập sai quá nhiều lần. Hãy yêu cầu mã mới.");
			case INVALID -> throw new OtpInvalidException("Mã OTP không đúng.");
		}
		throw new OtpInvalidException("Mã OTP không đúng.");
	}

	public OwnerAccount verifyTotp(String code) {
		OwnerAccount owner = accountService.requireOwner();
		if (!owner.isTotpEnabled()) {
			throw new TotpNotEnabledException();
		}
		if (!Totp.verify(owner.getTotpSecret(), code, Instant.now(clock))) {
			throw new TotpInvalidException();
		}
		return owner;
	}

	public String beginTotpSetup(OwnerAccount owner) {
		String secret = Totp.generateSecret();
		totpPendingStore.put(owner.getPhone(), secret);
		return "otpauth://totp/RoomManagement:%s?secret=%s&issuer=RoomManagement&algorithm=SHA1&digits=6&period=30"
			.formatted(owner.getPhone(), secret);
	}

	public void confirmTotpSetup(OwnerAccount owner, String code) {
		String secret = totpPendingStore.take(owner.getPhone());
		if (secret == null || !Totp.verify(secret, code, Instant.now(clock))) {
			throw new TotpInvalidException();
		}
		accountService.setTotpSecret(owner, secret);
	}

	public void disableTotp(OwnerAccount owner) {
		accountService.setTotpSecret(owner, null);
	}

	public record OtpIssued(long expiresInSeconds) {
	}
}
