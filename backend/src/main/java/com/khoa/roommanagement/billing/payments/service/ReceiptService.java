package com.khoa.roommanagement.billing.payments.service;

import com.khoa.roommanagement.billing.payments.entity.Payment;
import com.khoa.roommanagement.billing.payments.entity.Receipt;
import com.khoa.roommanagement.billing.payments.exception.InvalidReceiptFileException;
import com.khoa.roommanagement.billing.payments.exception.PaymentNotFoundException;
import com.khoa.roommanagement.billing.payments.exception.ReceiptNotFoundException;
import com.khoa.roommanagement.billing.payments.repository.PaymentRepository;
import com.khoa.roommanagement.billing.payments.repository.ReceiptRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReceiptService {

	private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

	private final ReceiptRepository receiptRepository;
	private final PaymentRepository paymentRepository;
	private final ReceiptFileValidator fileValidator;
	private final Path storageRoot;

	public ReceiptService(
		ReceiptRepository receiptRepository,
		PaymentRepository paymentRepository,
		ReceiptFileValidator fileValidator,
		@Value("${app.receipts.storage-path:./receipts}") String storagePath
	) {
		this.receiptRepository = receiptRepository;
		this.paymentRepository = paymentRepository;
		this.fileValidator = fileValidator;
		this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
	}

	@Transactional
	public Receipt upload(UUID paymentId, MultipartFile file) {
		Payment payment = paymentRepository.findById(paymentId)
			.orElseThrow(() -> new PaymentNotFoundException(paymentId));

		if (receiptRepository.findByPaymentId(paymentId).isPresent()) {
			throw new InvalidReceiptFileException("Hóa đơn này đã có chứng từ.");
		}

		String contentType;
		try {
			contentType = fileValidator.validate(file);
		} catch (IOException e) {
			log.warn("Failed to read uploaded file");
			throw new InvalidReceiptFileException("Không thể đọc tệp chứng từ.");
		}

		String storedFileName = UUID.randomUUID() + extensionFor(contentType);
		Path targetPath = resolveSafePath(storedFileName);

		try {
			Files.createDirectories(targetPath.getParent());
			try (InputStream is = file.getInputStream()) {
				Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			log.error("Failed to store receipt file");
			throw new InvalidReceiptFileException("Không thể lưu tệp chứng từ.");
		}

		try {
			Receipt receipt = Receipt.create(
				payment,
				sanitizeOriginalFileName(file.getOriginalFilename()),
				storedFileName,
				contentType,
				file.getSize()
			);
			return receiptRepository.save(receipt);
		} catch (RuntimeException e) {
			deleteQuietly(targetPath);
			throw e;
		}
	}

	@Transactional(readOnly = true)
	public Receipt getById(UUID id) {
		return receiptRepository.findById(id)
			.orElseThrow(ReceiptNotFoundException::new);
	}

	public Resource loadFile(Receipt receipt) {
		Path filePath = resolveSafePath(receipt.getStoredFileName());
		if (!Files.exists(filePath)) {
			throw new ReceiptNotFoundException();
		}
		return new InputStreamResource(inputStreamOf(filePath));
	}

	private InputStream inputStreamOf(Path filePath) {
		try {
			return Files.newInputStream(filePath);
		} catch (IOException e) {
			throw new ReceiptNotFoundException();
		}
	}

	/**
	 * Resolve tên tệp dưới thư mục gốc và kiểm tra nó không thoát ra ngoài
	 * (path traversal protection).
	 */
	private Path resolveSafePath(String fileName) {
		Path resolved = storageRoot.resolve(fileName).normalize();
		if (!resolved.startsWith(storageRoot)) {
			log.warn("Blocked path traversal attempt");
			throw new InvalidReceiptFileException("Tên tệp không hợp lệ.");
		}
		return resolved;
	}

	private String sanitizeOriginalFileName(String original) {
		if (original == null || original.isBlank()) {
			return "receipt";
		}
		String name = Path.of(original).getFileName().toString();
		return name.length() > 200 ? name.substring(name.length() - 200) : name;
	}

	private String extensionFor(String contentType) {
		return switch (contentType) {
			case "image/jpeg" -> ".jpg";
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> ".bin";
		};
	}

	private void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.warn("Failed to clean up receipt file after error");
		}
	}
}
