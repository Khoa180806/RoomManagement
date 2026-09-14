package com.khoa.roommanagement.billing;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record CreateRentalContractRequest(
	@NotNull(message = "Ngày bắt đầu là bắt buộc") LocalDate startDate,
	@NotNull(message = "Ngày kết thúc là bắt buộc") LocalDate endDate,
	@Min(value = 1, message = "Ngày đến hạn phải từ 1 đến 28")
	@Max(value = 28, message = "Ngày đến hạn phải từ 1 đến 28") int paymentDueDay,
	@PositiveOrZero(message = "Tiền phòng không được âm") long rentAmount,
	@PositiveOrZero(message = "Đơn giá điện không được âm") long electricityUnitPrice,
	@PositiveOrZero(message = "Tiền nước không được âm") long waterFee,
	@PositiveOrZero(message = "Phí dịch vụ không được âm") long serviceFee
) {

	@AssertTrue(message = "Ngày kết thúc phải sau ngày bắt đầu")
	public boolean isEndDateAfterStartDate() {
		return startDate != null && endDate != null && endDate.isAfter(startDate);
	}

	CreateRentalContractCommand toCommand() {
		return new CreateRentalContractCommand(
			startDate,
			endDate,
			paymentDueDay,
			rentAmount,
			electricityUnitPrice,
			waterFee,
			serviceFee
		);
	}
}
