package com.khoa.roommanagement.billing.payments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "receipts")
public class Receipt {

	@Id
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "payment_id", nullable = false, unique = true)
	private Payment payment;

	@Column(name = "original_file_name", nullable = false)
	private String originalFileName;

	@Column(name = "stored_file_name", nullable = false, unique = true)
	private String storedFileName;

	@Column(name = "content_type", nullable = false)
	private String contentType;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Receipt() {
	}

	public static Receipt create(Payment payment, String originalFileName, String storedFileName,
			String contentType, long fileSize) {
		Receipt receipt = new Receipt();
		receipt.id = UUID.randomUUID();
		receipt.payment = payment;
		receipt.originalFileName = originalFileName;
		receipt.storedFileName = storedFileName;
		receipt.contentType = contentType;
		receipt.fileSize = fileSize;
		receipt.createdAt = Instant.now();
		return receipt;
	}

	public UUID getId() {
		return id;
	}

	public Payment getPayment() {
		return payment;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public String getStoredFileName() {
		return storedFileName;
	}

	public String getContentType() {
		return contentType;
	}

	public long getFileSize() {
		return fileSize;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
