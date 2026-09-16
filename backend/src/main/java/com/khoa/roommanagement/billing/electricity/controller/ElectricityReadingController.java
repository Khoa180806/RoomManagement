package com.khoa.roommanagement.billing.electricity.controller;

import com.khoa.roommanagement.billing.electricity.dto.CreateElectricityReadingRequest;
import com.khoa.roommanagement.billing.electricity.dto.ElectricityReadingResponse;
import com.khoa.roommanagement.billing.electricity.service.ElectricityReadingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/electricity-readings")
public class ElectricityReadingController {

	private final ElectricityReadingService readingService;

	public ElectricityReadingController(ElectricityReadingService readingService) {
		this.readingService = readingService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ElectricityReadingResponse record(@Valid @RequestBody CreateElectricityReadingRequest request) {
		return ElectricityReadingResponse.from(readingService.record(request.toCommand()));
	}

	@GetMapping
	public List<ElectricityReadingResponse> getReadings() {
		return readingService.getReadingsByActiveContract().stream()
			.map(ElectricityReadingResponse::from)
			.toList();
	}
}
