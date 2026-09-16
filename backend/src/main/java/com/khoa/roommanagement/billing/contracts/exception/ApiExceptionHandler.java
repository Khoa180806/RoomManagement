package com.khoa.roommanagement.billing.contracts.exception;

import com.khoa.roommanagement.billing.bills.exception.BillNotFoundException;
import com.khoa.roommanagement.billing.bills.exception.DuplicateBillException;
import com.khoa.roommanagement.billing.bills.exception.PreviousReadingNotFoundException;
import com.khoa.roommanagement.billing.bills.exception.ReadingNotFoundException;
import com.khoa.roommanagement.billing.electricity.exception.DuplicateReadingException;
import com.khoa.roommanagement.billing.electricity.exception.MeterValueDecreasedException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		List<String> details = exception.getBindingResult().getAllErrors().stream()
			.map(error -> error.getDefaultMessage())
			.toList();
		return ResponseEntity.unprocessableEntity()
			.body(ApiErrorResponse.of("VALIDATION_ERROR", "Dữ liệu không hợp lệ", details));
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
}
