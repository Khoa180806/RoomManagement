package com.khoa.roommanagement.common.exception;

import com.khoa.roommanagement.billing.bills.exception.BillNotFoundException;
import com.khoa.roommanagement.billing.bills.exception.DuplicateBillException;
import com.khoa.roommanagement.billing.bills.exception.PreviousReadingNotFoundException;
import com.khoa.roommanagement.billing.bills.exception.ReadingNotFoundException;
import com.khoa.roommanagement.billing.contracts.exception.ActiveRentalContractAlreadyExistsException;
import com.khoa.roommanagement.billing.contracts.exception.ApiErrorResponse;
import com.khoa.roommanagement.billing.contracts.exception.ContractTerminatedException;
import com.khoa.roommanagement.billing.contracts.exception.RentalContractNotFoundException;
import com.khoa.roommanagement.billing.electricity.exception.DuplicateReadingException;
import com.khoa.roommanagement.billing.electricity.exception.MeterValueDecreasedException;
import com.khoa.roommanagement.billing.electricity.exception.NonConsecutivePeriodException;
import com.khoa.roommanagement.billing.payments.exception.BillAlreadyPaidException;
import com.khoa.roommanagement.billing.payments.exception.IdempotencyConflictException;
import com.khoa.roommanagement.billing.payments.exception.InvalidPaidAtException;
import com.khoa.roommanagement.billing.payments.exception.InvalidReceiptFileException;
import com.khoa.roommanagement.billing.payments.exception.PaymentNotFoundException;
import com.khoa.roommanagement.billing.payments.exception.ReceiptNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		List<String> details = exception.getBindingResult().getAllErrors().stream()
				.map(error -> error.getDefaultMessage())
				.toList();
		return ResponseEntity.unprocessableEntity()
				.body(ApiErrorResponse.of("VALIDATION_ERROR", "Dữ liệu không hợp lệ", details));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiErrorResponse> handleUnreadableMessage() {
		return ResponseEntity.badRequest()
				.body(ApiErrorResponse.of("INVALID_JSON", "Dữ liệu JSON không hợp lệ", List.of()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		log.warn("Data integrity violation: {}", exception.getMessage());
		String message = exception.getMessage();
		if (message != null && message.contains("uq_rental_contracts_active_marker")) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(ApiErrorResponse.of("ACTIVE_CONTRACT_EXISTS", "Đã có hợp đồng đang hiệu lực", List.of()));
		}
		if (message != null && message.contains("uq_electricity_readings_contract_period")) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(ApiErrorResponse.of("DUPLICATE_READING", "Đã có chỉ số điện cho kỳ này", List.of()));
		}
		if (message != null && message.contains("uq_bills_contract_period")) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(ApiErrorResponse.of("DUPLICATE_BILL", "Đã có hóa đơn cho kỳ này", List.of()));
		}
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiErrorResponse.of("DATA_INTEGRITY_VIOLATION", "Dữ liệu bị trùng hoặc vi phạm ràng buộc", List.of()));
	}

	@ExceptionHandler(ActiveRentalContractAlreadyExistsException.class)
	ResponseEntity<ApiErrorResponse> handleActiveContractAlreadyExists() {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiErrorResponse.of("ACTIVE_CONTRACT_EXISTS", "Đã có hợp đồng đang hiệu lực", List.of()));
	}

	@ExceptionHandler(RentalContractNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleActiveContractNotFound() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiErrorResponse.of("NOT_FOUND", "Chưa có hợp đồng đang hiệu lực", List.of()));
	}

	@ExceptionHandler(ContractTerminatedException.class)
	ResponseEntity<ApiErrorResponse> handleContractTerminated(ContractTerminatedException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiErrorResponse.of("CONTRACT_TERMINATED", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(DuplicateReadingException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateReading(DuplicateReadingException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiErrorResponse.of("DUPLICATE_READING", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(MeterValueDecreasedException.class)
	ResponseEntity<ApiErrorResponse> handleMeterValueDecreased(MeterValueDecreasedException exception) {
		return ResponseEntity.unprocessableEntity()
				.body(ApiErrorResponse.of("METER_VALUE_DECREASED", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(NonConsecutivePeriodException.class)
	ResponseEntity<ApiErrorResponse> handleNonConsecutivePeriod(NonConsecutivePeriodException exception) {
		return ResponseEntity.unprocessableEntity()
				.body(ApiErrorResponse.of("NON_CONSECUTIVE_PERIOD", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(DuplicateBillException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateBill(DuplicateBillException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiErrorResponse.of("DUPLICATE_BILL", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(ReadingNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleReadingNotFound(ReadingNotFoundException exception) {
		return ResponseEntity.unprocessableEntity()
				.body(ApiErrorResponse.of("READING_NOT_FOUND", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(PreviousReadingNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handlePreviousReadingNotFound(PreviousReadingNotFoundException exception) {
		return ResponseEntity.unprocessableEntity()
				.body(ApiErrorResponse.of("PREVIOUS_READING_NOT_FOUND", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(BillNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleBillNotFound(BillNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiErrorResponse.of("BILL_NOT_FOUND", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(BillAlreadyPaidException.class)
	ResponseEntity<ApiErrorResponse> handleBillAlreadyPaid(BillAlreadyPaidException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiErrorResponse.of("BILL_ALREADY_PAID", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(InvalidPaidAtException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidPaidAt(InvalidPaidAtException exception) {
		return ResponseEntity.unprocessableEntity()
				.body(ApiErrorResponse.of("INVALID_PAID_AT", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(IdempotencyConflictException.class)
	ResponseEntity<ApiErrorResponse> handleIdempotencyConflict(IdempotencyConflictException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ApiErrorResponse.of("IDEMPOTENCY_CONFLICT", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(PaymentNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handlePaymentNotFound(PaymentNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiErrorResponse.of("PAYMENT_NOT_FOUND", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(InvalidReceiptFileException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidReceiptFile(InvalidReceiptFileException exception) {
		return ResponseEntity.unprocessableEntity()
				.body(ApiErrorResponse.of("INVALID_RECEIPT_FILE", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(ReceiptNotFoundException.class)
	ResponseEntity<ApiErrorResponse> handleReceiptNotFound(ReceiptNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiErrorResponse.of("RECEIPT_NOT_FOUND", exception.getMessage(), List.of()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleGenericException(Exception exception) {
		log.error("Unhandled exception", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiErrorResponse.of("INTERNAL_ERROR", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.", List.of()));
	}
}
