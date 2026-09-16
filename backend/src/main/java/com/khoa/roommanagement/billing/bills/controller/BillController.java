package com.khoa.roommanagement.billing.bills.controller;

import com.khoa.roommanagement.billing.bills.dto.BillResponse;
import com.khoa.roommanagement.billing.bills.dto.CreateBillRequest;
import com.khoa.roommanagement.billing.bills.service.BillService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bills")
public class BillController {

	private final BillService billService;

	public BillController(BillService billService) {
		this.billService = billService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BillResponse create(@Valid @RequestBody CreateBillRequest request) {
		return BillResponse.from(billService.create(request.toCommand()));
	}

	@GetMapping("/{id}")
	public BillResponse getById(@PathVariable UUID id) {
		return BillResponse.from(billService.getById(id));
	}

	@GetMapping
	public Page<BillResponse> getBills(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "12") int size,
		@RequestParam(required = false) String period
	) {
		return billService.getBills(page, size, period).map(BillResponse::from);
	}
}
