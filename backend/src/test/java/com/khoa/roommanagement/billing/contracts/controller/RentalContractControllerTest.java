package com.khoa.roommanagement.billing.contracts.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khoa.roommanagement.billing.contracts.exception.ApiExceptionHandler;
import com.khoa.roommanagement.billing.contracts.exception.ActiveRentalContractAlreadyExistsException;
import com.khoa.roommanagement.billing.contracts.service.RentalContractService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RentalContractController.class)
@Import(ApiExceptionHandler.class)
class RentalContractControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RentalContractService rentalContractService;

	@Test
	void returnsStructuredValidationErrorForInvalidContract() throws Exception {
		mockMvc.perform(post("/api/contracts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest().replace("3500000", "-1")))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void returnsConflictWhenAnActiveContractAlreadyExists() throws Exception {
		when(rentalContractService.create(any())).thenThrow(new ActiveRentalContractAlreadyExistsException());

		mockMvc.perform(post("/api/contracts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validRequest()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("ACTIVE_CONTRACT_EXISTS"));
	}

	private String validRequest() {
		return """
			{
			  "startDate": "2026-09-01",
			  "endDate": "2027-08-31",
			  "paymentDueDay": 5,
			  "rentAmount": 3500000,
			  "electricityUnitPrice": 4000,
			  "waterFee": 100000,
			  "serviceFee": 150000
			}
			""";
	}
}
