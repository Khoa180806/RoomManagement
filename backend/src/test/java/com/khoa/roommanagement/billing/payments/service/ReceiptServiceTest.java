package com.khoa.roommanagement.billing.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.contracts.dto.CreateRentalContractCommand;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.payments.entity.Payment;
import com.khoa.roommanagement.billing.payments.entity.Receipt;
import com.khoa.roommanagement.billing.payments.exception.InvalidReceiptFileException;
import com.khoa.roommanagement.billing.payments.exception.PaymentNotFoundException;
import com.khoa.roommanagement.billing.payments.exception.ReceiptNotFoundException;
import com.khoa.roommanagement.billing.payments.repository.PaymentRepository;
import com.khoa.roommanagement.billing.payments.repository.ReceiptRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

	@Mock
	private ReceiptRepository receiptRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private ReceiptFileValidator fileValidator;

	@TempDir
	Path tempDir;

	private ReceiptService receiptService;

	private Payment payment;

	@BeforeEach
	void setUp() {
		receiptService = new ReceiptService(receiptRepository, paymentRepository, fileValidator,
				tempDir.toString());

		RentalContract contract = RentalContract.createActive(
			new CreateRentalContractCommand(
				LocalDate.of(2026, 2, 2),
				LocalDate.of(2027, 2, 2),
				4,
				4_400_000L,
				3_800L,
				200_000L,
				100_000L
			)
		);
		Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-08");
		payment = Payment.confirm(bill, Instant.now().minusSeconds(86400), null, "key-1");
	}

	@Test
	void uploadsValidReceipt() throws IOException {
		UUID paymentId = payment.getId();
		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(receiptRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
		when(fileValidator.detectContentType(any())).thenReturn("image/jpeg");
		when(receiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		byte[] bytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
		MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg",
				new ByteArrayInputStream(bytes));

		Receipt receipt = receiptService.upload(paymentId, file);

		assertThat(receipt.getPayment().getId()).isEqualTo(paymentId);
		assertThat(receipt.getContentType()).isEqualTo("image/jpeg");
		assertThat(receipt.getStoredFileName()).endsWith(".jpg");
		assertThat(receipt.getOriginalFileName()).isEqualTo("photo.jpg");

		Path stored = tempDir.resolve(receipt.getStoredFileName());
		assertThat(Files.exists(stored)).isTrue();
	}

	@Test
	void throwsWhenPaymentNotFound() throws IOException {
		UUID paymentId = UUID.randomUUID();
		when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

		MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg",
				new ByteArrayInputStream(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));

		assertThatThrownBy(() -> receiptService.upload(paymentId, file))
			.isInstanceOf(PaymentNotFoundException.class);
	}

	@Test
	void throwsWhenReceiptAlreadyExists() throws IOException {
		UUID paymentId = payment.getId();
		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(receiptRepository.findByPaymentId(paymentId))
			.thenReturn(Optional.of(Receipt.create(payment, "a.jpg", "uuid.jpg", "image/jpeg", 100)));

		MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg",
				new ByteArrayInputStream(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));

		assertThatThrownBy(() -> receiptService.upload(paymentId, file))
			.isInstanceOf(InvalidReceiptFileException.class);
	}

	@Test
	void throwsWhenValidatorRejects() throws IOException {
		UUID paymentId = payment.getId();
		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(receiptRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
		org.mockito.Mockito.doThrow(new InvalidReceiptFileException("Chỉ chấp nhận ảnh JPEG, PNG hoặc WebP."))
			.when(fileValidator).validate(any());

		MockMultipartFile file = new MockMultipartFile("file", "bad.pdf", "application/pdf",
				new ByteArrayInputStream("pdf".getBytes()));

		assertThatThrownBy(() -> receiptService.upload(paymentId, file))
			.isInstanceOf(InvalidReceiptFileException.class);
	}

	@Test
	void loadsStoredFile() throws IOException {
		Receipt receipt = Receipt.create(payment, "a.jpg", "stored-uuid.jpg", "image/jpeg", 4);
		Files.write(tempDir.resolve("stored-uuid.jpg"), new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

		var resource = receiptService.loadFile(receipt);

		assertThat(resource.exists()).isTrue();
		assertThat(resource.contentLength()).isEqualTo(3);
	}

	@Test
	void throwsWhenFileMissingOnDisk() {
		Receipt receipt = Receipt.create(payment, "a.jpg", "missing.jpg", "image/jpeg", 4);

		assertThatThrownBy(() -> receiptService.loadFile(receipt))
			.isInstanceOf(ReceiptNotFoundException.class);
	}
}
