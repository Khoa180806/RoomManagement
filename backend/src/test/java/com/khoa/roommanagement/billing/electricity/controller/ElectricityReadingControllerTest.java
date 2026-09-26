package com.khoa.roommanagement.billing.electricity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khoa.roommanagement.common.exception.ApiExceptionHandler;
import com.khoa.roommanagement.billing.contracts.exception.RentalContractNotFoundException;
import com.khoa.roommanagement.billing.electricity.dto.ElectricityReadingResponse;
import com.khoa.roommanagement.billing.electricity.exception.DuplicateReadingException;
import com.khoa.roommanagement.billing.electricity.exception.MeterValueDecreasedException;
import com.khoa.roommanagement.billing.electricity.service.ElectricityReadingService;
import com.khoa.roommanagement.billing.electricity.entity.ElectricityReading;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ElectricityReadingController.class)
@Import(ApiExceptionHandler.class)
class ElectricityReadingControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ElectricityReadingService readingService;

	@Test
	void returnsValidationErrorForInvalidPeriod() throws Exception {
		mockMvc.perform(post("/api/electricity-readings")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "period": "invalid",
					  "meterValue": 1200
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void returnsValidationErrorForNegativeMeterValue() throws Exception {
		mockMvc.perform(post("/api/electricity-readings")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "period": "2026-09",
					  "meterValue": -1
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void returnsConflictForDuplicateReading() throws Exception {
		when(readingService.record(any())).thenThrow(new DuplicateReadingException("2026-09"));

		mockMvc.perform(post("/api/electricity-readings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validReadingRequest()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("DUPLICATE_READING"));
	}

	@Test
	void returnsUnprocessableForMeterValueDecrease() throws Exception {
		when(readingService.record(any())).thenThrow(new MeterValueDecreasedException(1000L, 1200L));

		mockMvc.perform(post("/api/electricity-readings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validReadingRequest()))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.error.code").value("METER_VALUE_DECREASED"));
	}

	@Test
	void returnsNotFoundWhenNoActiveContract() throws Exception {
		when(readingService.record(any())).thenThrow(new RentalContractNotFoundException());

		mockMvc.perform(post("/api/electricity-readings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validReadingRequest()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	@Test
	void createsReadingSuccessfully() throws Exception {
		UUID contractId = UUID.randomUUID();
		UUID readingId = UUID.randomUUID();
		ElectricityReading reading = ElectricityReading.record(contractId, "2026-09", 1200L);
		when(readingService.record(any())).thenReturn(reading);

		mockMvc.perform(post("/api/electricity-readings")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validReadingRequest()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.period").value("2026-09"))
			.andExpect(jsonPath("$.meterValue").value(1200));
	}

	@Test
	void returnsReadingsList() throws Exception {
		UUID contractId = UUID.randomUUID();
		ElectricityReading reading1 = ElectricityReading.record(contractId, "2026-10", 1400L);
		ElectricityReading reading2 = ElectricityReading.record(contractId, "2026-09", 1200L);
		when(readingService.getReadingsByActiveContract()).thenReturn(List.of(reading1, reading2));

		mockMvc.perform(get("/api/electricity-readings"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].period").value("2026-10"))
			.andExpect(jsonPath("$[1].period").value("2026-09"));
	}

	private String validReadingRequest() {
		return """
			{
			  "period": "2026-09",
			  "meterValue": 1200
			}
			""";
	}
}
