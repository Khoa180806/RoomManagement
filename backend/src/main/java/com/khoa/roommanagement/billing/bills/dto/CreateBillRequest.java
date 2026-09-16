package com.khoa.roommanagement.billing.bills.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateBillRequest(
	@NotBlank(message = "Kỳ là bắt buộc")
	@Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Kỳ phải có định dạng YYYY-MM")
	String period
) {

	public CreateBillCommand toCommand() {
		return new CreateBillCommand(period);
	}
}
