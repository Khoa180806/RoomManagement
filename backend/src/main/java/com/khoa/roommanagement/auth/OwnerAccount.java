package com.khoa.roommanagement.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "owner_account")
public class OwnerAccount {

	@Id
	private UUID id;

	/** Chuỗi số đã chuẩn hóa (bỏ đầu 0/84) để so khớp SĐT. */
	@Column(nullable = false, unique = true, length = 20)
	private String phone;

	/** Secret Base32 cho TOTP dự phòng; null = chưa bật. */
	@Column(name = "totp_secret", columnDefinition = "TEXT")
	private String totpSecret;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected OwnerAccount() {
	}

	public static OwnerAccount create(String phone) {
		OwnerAccount account = new OwnerAccount();
		account.id = UUID.randomUUID();
		account.phone = phone;
		account.createdAt = Instant.now();
		account.updatedAt = Instant.now();
		return account;
	}

	public void updatePhone(String phone) {
		this.phone = phone;
		this.updatedAt = Instant.now();
	}

	public void setTotpSecret(String totpSecret) {
		this.totpSecret = totpSecret;
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getPhone() {
		return phone;
	}

	public String getTotpSecret() {
		return totpSecret;
	}

	public boolean isTotpEnabled() {
		return totpSecret != null && !totpSecret.isBlank();
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
