package com.khoa.roommanagement.billing.bills.dto;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.entity.BillStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BillResponse(
	UUID id,
	UUID contractId,
	String period,
	long rentAmount,
	long electricityUnitPrice,
	long waterFee,
	long serviceFee,
	long oldMeterValue,
	long newMeterValue,
	long consumption,
	long electricityAmount,
	long totalAmount,
	BillStatus status,
	LocalDate dueDate,
	Instant createdAt
) {

	public static BillResponse from(Bill bill) {
		return new BillResponse(
			bill.getId(),
			bill.getContractId(),
			bill.getPeriod(),
			bill.getRentAmount(),
			bill.getElectricityUnitPrice(),
			bill.getWaterFee(),
			bill.getServiceFee(),
			bill.getOldMeterValue(),
			bill.getNewMeterValue(),
			bill.getConsumption(),
			bill.getElectricityAmount(),
			bill.getTotalAmount(),
			bill.getStatus(),
			bill.getDueDate(),
			bill.getCreatedAt()
		);
	}
}
