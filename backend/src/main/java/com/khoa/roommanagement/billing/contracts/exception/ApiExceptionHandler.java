package com.khoa.roommanagement.billing.contracts.exception;

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
}
