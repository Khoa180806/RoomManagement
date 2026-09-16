package com.khoa.roommanagement.billing.electricity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateElectricityReadingRequest(
	@NotBlank(message = "Kỳ là bắt buộc")
	@Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Kỳ phải có định dạng YYYY-MM")
	String period,
	@NotNull(message = "Chỉ số điện là bắt buộc")
	@PositiveOrZero(message = "Chỉ số điện không được âm")
	Long meterValue
) {

	public CreateReadingCommand toCommand() {
		return new CreateReadingCommand(period, meterValue);
	}
}
