package com.khoa.roommanagement.billing.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.khoa.roommanagement.billing.payments.exception.InvalidReceiptFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ReceiptFileValidatorTest {

	@InjectMocks
	private ReceiptFileValidator validator;

	@Mock
	private MultipartFile file;

	@Test
	void acceptsValidJpeg() throws IOException {
		byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
		MockMultipartFile multipart = new MockMultipartFile("file", "photo.jpg", "image/jpeg",
				new ByteArrayInputStream(jpegBytes));

		String type = validator.detectContentType(multipart);

		assertThat(type).isEqualTo("image/jpeg");
	}

	@Test
	void acceptsValidPng() throws IOException {
		byte[] pngBytes = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
		MockMultipartFile multipart = new MockMultipartFile("file", "photo.png", "image/png",
				new ByteArrayInputStream(pngBytes));

		String type = validator.detectContentType(multipart);

		assertThat(type).isEqualTo("image/png");
	}

	@Test
	void acceptsValidWebp() throws IOException {
		byte[] webpBytes = {0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};
		MockMultipartFile multipart = new MockMultipartFile("file", "photo.webp", "image/webp",
				new ByteArrayInputStream(webpBytes));

		String type = validator.detectContentType(multipart);

		assertThat(type).isEqualTo("image/webp");
	}

	@Test
	void rejectsFakeMimeWithTextContent() throws IOException {
		// File nội dung text nhưng khai báo MIME là image/jpeg
		byte[] textBytes = "this is not an image at all".getBytes();
		MockMultipartFile multipart = new MockMultipartFile("file", "fake.jpg", "image/jpeg",
				new ByteArrayInputStream(textBytes));

		assertThatThrownBy(() -> validator.validate(multipart))
			.isInstanceOf(InvalidReceiptFileException.class);
	}

	@Test
	void rejectsPdfContent() throws IOException {
		byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
		MockMultipartFile multipart = new MockMultipartFile("file", "doc.pdf", "application/pdf",
				new ByteArrayInputStream(pdfBytes));

		assertThatThrownBy(() -> validator.validate(multipart))
			.isInstanceOf(InvalidReceiptFileException.class);
	}

	@Test
	void rejectsEmptyFile() throws IOException {
		MockMultipartFile multipart = new MockMultipartFile("file", "empty.jpg", "image/jpeg",
				new ByteArrayInputStream(new byte[0]));

		assertThatThrownBy(() -> validator.validate(multipart))
			.isInstanceOf(InvalidReceiptFileException.class);
	}

	@Test
	void rejectsFileExceedingMaxSize() throws IOException {
		byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
		MockMultipartFile multipart = new MockMultipartFile("file", "big.jpg", "image/jpeg",
				new ByteArrayInputStream(jpegBytes));

		when(file.isEmpty()).thenReturn(false);
		when(file.getSize()).thenReturn(ReceiptFileValidator.MAX_FILE_SIZE + 1);

		assertThatThrownBy(() -> validator.validate(file))
			.isInstanceOf(InvalidReceiptFileException.class);
	}

	@Test
	void detectsTypeFromInputStreamPrefix() throws IOException {
		byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
		InputStream is = new ByteArrayInputStream(jpegBytes);

		// Chỉ 3 byte JPEG header cũng đủ nhận diện
		MockMultipartFile multipart = new MockMultipartFile("file", "x.jpg", "image/jpeg",
				new ByteArrayInputStream(jpegBytes));

		assertThat(validator.detectContentType(multipart)).isEqualTo("image/jpeg");
	}
}
