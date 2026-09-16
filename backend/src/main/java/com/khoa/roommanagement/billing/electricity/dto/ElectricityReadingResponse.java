package com.khoa.roommanagement.billing.electricity.dto;

import com.khoa.roommanagement.billing.electricity.entity.ElectricityReading;
import java.time.Instant;
import java.util.UUID;

public record ElectricityReadingResponse(
	UUID id,
	UUID contractId,
	String period,
	long meterValue,
	Instant recordedAt
) {

	public static ElectricityReadingResponse from(ElectricityReading reading) {
		return new ElectricityReadingResponse(
			reading.getId(),
			reading.getContractId(),
			reading.getPeriod(),
			reading.getMeterValue(),
			reading.getRecordedAt()
		);
	}
}
