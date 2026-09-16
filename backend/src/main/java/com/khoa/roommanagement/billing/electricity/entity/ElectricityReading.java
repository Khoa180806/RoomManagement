package com.khoa.roommanagement.billing.electricity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "electricity_readings")
public class ElectricityReading {

	@Id
	private UUID id;

	@Column(name = "contract_id", nullable = false)
	private UUID contractId;

	@Column(nullable = false)
	private String period;

	@Column(nullable = false)
	private long meterValue;

	@Column(nullable = false)
	private Instant recordedAt;

	protected ElectricityReading() {
	}

	public static ElectricityReading record(UUID contractId, String period, long meterValue) {
		ElectricityReading reading = new ElectricityReading();
		reading.id = UUID.randomUUID();
		reading.contractId = contractId;
		reading.period = period;
		reading.meterValue = meterValue;
		reading.recordedAt = Instant.now();
		return reading;
	}

	public UUID getId() {
		return id;
	}

	public UUID getContractId() {
		return contractId;
	}

	public String getPeriod() {
		return period;
	}

	public long getMeterValue() {
		return meterValue;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}
}
