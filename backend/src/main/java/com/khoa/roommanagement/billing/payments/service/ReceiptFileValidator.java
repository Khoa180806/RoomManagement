package com.khoa.roommanagement.billing.payments.service;

import com.khoa.roommanagement.billing.payments.exception.InvalidReceiptFileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Kiểm tra loại tệp dựa trên chữ ký tệp (magic bytes), không tin MIME type
 * do client gửi. Chỉ chấp nhận JPEG, PNG và WebP.
 */
@Component
public class ReceiptFileValidator {

	public static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB

	private static final Map<String, String> SUPPORTED_TYPES = Map.of(
			"image/jpeg", "JPEG",
			"image/png", "PNG",
			"image/webp", "WEBP"
	);

	public record ValidationResult(String contentType, String formatName) {
	}

	public void validate(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new InvalidReceiptFileException("Tệp chứng từ là bắt buộc.");
		}

		if (file.getSize() > MAX_FILE_SIZE) {
			throw new InvalidReceiptFileException("Tệp chứng từ không được vượt quá 5 MB.");
		}

		String detectedType = detectContentType(file);
		if (detectedType == null || !SUPPORTED_TYPES.containsKey(detectedType)) {
			throw new InvalidReceiptFileException("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP.");
		}
	}

	public String detectContentType(MultipartFile file) throws IOException {
		try (InputStream is = file.getInputStream()) {
			return detectFromMagicBytes(is);
		}
	}

	private String detectFromMagicBytes(InputStream is) throws IOException {
		byte[] header = new byte[12];
		int read = is.readNBytes(header, 0, header.length);
		if (read < 3) {
			return null;
		}

		// JPEG: FF D8 FF
		if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
			return "image/jpeg";
		}

		// PNG: 89 50 4E 47 0D 0A 1A 0A
		if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
			return "image/png";
		}

		// WebP: RIFF....WEBP
		if (read >= 12 && header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
				&& header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
			return "image/webp";
		}

		return null;
	}
}
