package com.khoa.roommanagement.billing.bills.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.exception.BillNotFoundException;
import com.khoa.roommanagement.billing.bills.exception.DuplicateBillException;
import com.khoa.roommanagement.billing.bills.exception.ReadingNotFoundException;
import com.khoa.roommanagement.billing.bills.service.BillService;
import com.khoa.roommanagement.billing.contracts.dto.CreateRentalContractCommand;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.common.exception.ApiExceptionHandler;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(BillController.class)
@Import(ApiExceptionHandler.class)
class BillControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BillService billService;

	@Test
	void returnsValidationErrorForInvalidPeriod() throws Exception {
		mockMvc.perform(post("/api/bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "period": "invalid"
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void returnsConflictForDuplicateBill() throws Exception {
		when(billService.create(any())).thenThrow(new DuplicateBillException("2026-10"));

		mockMvc.perform(post("/api/bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBillRequest()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.error.code").value("DUPLICATE_BILL"));
	}

	@Test
	void returnsUnprocessableWhenNoReading() throws Exception {
		when(billService.create(any())).thenThrow(new ReadingNotFoundException("2026-10"));

		mockMvc.perform(post("/api/bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBillRequest()))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.error.code").value("READING_NOT_FOUND"));
	}

	@Test
	void createsBillSuccessfully() throws Exception {
		RentalContract contract = RentalContract.createActive(
			new CreateRentalContractCommand(
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2027, 8, 31),
				5,
				3_500_000L,
				4_000L,
				100_000L,
				150_000L
			)
		);
		Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-10");
		when(billService.create(any())).thenReturn(bill);

		mockMvc.perform(post("/api/bills")
				.contentType(MediaType.APPLICATION_JSON)
				.content(validBillRequest()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.period").value("2026-10"))
			.andExpect(jsonPath("$.consumption").value(200))
			.andExpect(jsonPath("$.totalAmount").value(4550000))
			.andExpect(jsonPath("$.status").value("PENDING"));
	}

	@Test
	void returnsNotFoundForNonExistentBill() throws Exception {
		UUID id = UUID.randomUUID();
		when(billService.getById(id)).thenThrow(new BillNotFoundException(id));

		mockMvc.perform(get("/api/bills/" + id))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.error.code").value("BILL_NOT_FOUND"));
	}

	@Test
	void returnsBillsPage() throws Exception {
		RentalContract contract = RentalContract.createActive(
			new CreateRentalContractCommand(
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2027, 8, 31),
				5,
				3_500_000L,
				4_000L,
				100_000L,
				150_000L
			)
		);
		Bill bill = Bill.createFrom(contract, 1200L, 1000L, "2026-10");
		Page<Bill> page = new PageImpl<>(java.util.List.of(bill), PageRequest.of(0, 12), 1);
		when(billService.getBills(anyInt(), anyInt(), any())).thenReturn(page);

		mockMvc.perform(get("/api/bills"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(1))
			.andExpect(jsonPath("$.content[0].period").value("2026-10"));
	}

	private String validBillRequest() {
		return """
			{
			  "period": "2026-10"
			}
			""";
	}
}
