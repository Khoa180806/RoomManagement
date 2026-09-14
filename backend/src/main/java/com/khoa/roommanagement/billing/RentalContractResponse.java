package com.khoa.roommanagement.billing;

import java.time.LocalDate;
import java.util.UUID;

public record RentalContractResponse(
	UUID id,
	LocalDate startDate,
	LocalDate endDate,
	int paymentDueDay,
	long rentAmount,
	long electricityUnitPrice,
	long waterFee,
	long serviceFee,
	RentalContractStatus status
) {

	static RentalContractResponse from(RentalContract contract) {
		return new RentalContractResponse(
			contract.getId(),
			contract.getStartDate(),
			contract.getEndDate(),
			contract.getPaymentDueDay(),
			contract.getRentAmount(),
			contract.getElectricityUnitPrice(),
			contract.getWaterFee(),
			contract.getServiceFee(),
			contract.getStatus()
		);
	}
}
