package com.khoa.roommanagement.billing.payments.dto;

import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(
	UUID id,
	UUID paymentId,
	String originalFileName,
	String contentType,
	long fileSize,
	Instant createdAt,
	String downloadUrl
) {
	public static ReceiptResponse from(com.khoa.roommanagement.billing.payments.entity.Receipt receipt) {
		return new ReceiptResponse(
			receipt.getId(),
			receipt.getPayment().getId(),
			receipt.getOriginalFileName(),
			receipt.getContentType(),
			receipt.getFileSize(),
			receipt.getCreatedAt(),
			"/api/receipts/" + receipt.getId()
		);
	}
}
