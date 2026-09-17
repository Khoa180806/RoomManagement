package com.khoa.roommanagement.billing.contracts.entity;

import com.khoa.roommanagement.billing.contracts.dto.CreateRentalContractCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "rental_contracts")
public class RentalContract {

	@Id
	private UUID id;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	@Column(nullable = false)
	private int paymentDueDay;

	@Column(nullable = false)
	private long rentAmount;

	@Column(nullable = false)
	private long electricityUnitPrice;

	@Column(nullable = false)
	private long waterFee;

	@Column(nullable = false)
	private long serviceFee;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RentalContractStatus status;

	@Column
	private Boolean activeContractMarker;

	@Column(nullable = false)
	private Instant createdAt;

	protected RentalContract() {
	}

	public static RentalContract createActive(CreateRentalContractCommand command) {
		RentalContract contract = new RentalContract();
		contract.id = UUID.randomUUID();
		contract.startDate = command.startDate();
		contract.endDate = command.endDate();
		contract.paymentDueDay = command.paymentDueDay();
		contract.rentAmount = command.rentAmount();
		contract.electricityUnitPrice = command.electricityUnitPrice();
		contract.waterFee = command.waterFee();
		contract.serviceFee = command.serviceFee();
		contract.status = RentalContractStatus.ACTIVE;
		contract.activeContractMarker = true;
		contract.createdAt = Instant.now();
		return contract;
	}

	public UUID getId() {
		return id;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public int getPaymentDueDay() {
		return paymentDueDay;
	}

	public long getRentAmount() {
		return rentAmount;
	}

	public long getElectricityUnitPrice() {
		return electricityUnitPrice;
	}

	public long getWaterFee() {
		return waterFee;
	}

	public long getServiceFee() {
		return serviceFee;
	}

	public RentalContractStatus getStatus() {
		return status;
	}

	public void setStatus(RentalContractStatus status) {
		this.status = status;
		if (status != RentalContractStatus.ACTIVE) {
			this.activeContractMarker = null;
		}
	}
}
