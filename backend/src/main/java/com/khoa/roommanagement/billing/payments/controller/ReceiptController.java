package com.khoa.roommanagement.billing.payments.controller;

import com.khoa.roommanagement.billing.payments.dto.ReceiptResponse;
import com.khoa.roommanagement.billing.payments.entity.Receipt;
import com.khoa.roommanagement.billing.payments.service.ReceiptService;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ReceiptController {

	private final ReceiptService receiptService;

	public ReceiptController(ReceiptService receiptService) {
		this.receiptService = receiptService;
	}

	@PostMapping("/api/payments/{id}/receipts")
	@ResponseStatus(HttpStatus.CREATED)
	public ReceiptResponse upload(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
		return ReceiptResponse.from(receiptService.upload(id, file));
	}

	@GetMapping("/api/receipts/{id}")
	public ResponseEntity<Resource> download(@PathVariable UUID id) {
		Receipt receipt = receiptService.getById(id);
		Resource resource = receiptService.loadFile(receipt);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(receipt.getContentType()))
			.body(resource);
	}
}
