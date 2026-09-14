package com.khoa.roommanagement.billing.contracts.controller;

import com.khoa.roommanagement.billing.contracts.dto.CreateRentalContractRequest;
import com.khoa.roommanagement.billing.contracts.dto.RentalContractResponse;
import com.khoa.roommanagement.billing.contracts.service.RentalContractService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
public class RentalContractController {

	private final RentalContractService rentalContractService;

	public RentalContractController(RentalContractService rentalContractService) {
		this.rentalContractService = rentalContractService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RentalContractResponse create(@Valid @RequestBody CreateRentalContractRequest request) {
		return RentalContractResponse.from(rentalContractService.create(request.toCommand()));
	}

	@GetMapping("/active")
	public RentalContractResponse getActive() {
		return RentalContractResponse.from(rentalContractService.getActive());
	}
}
