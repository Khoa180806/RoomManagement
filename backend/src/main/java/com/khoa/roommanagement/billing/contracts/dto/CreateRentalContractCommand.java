package com.khoa.roommanagement.billing.contracts.dto;

import java.time.LocalDate;

public record CreateRentalContractCommand(
	LocalDate startDate,
	LocalDate endDate,
	int paymentDueDay,
	long rentAmount,
	long electricityUnitPrice,
	long waterFee,
	long serviceFee
) {
}
