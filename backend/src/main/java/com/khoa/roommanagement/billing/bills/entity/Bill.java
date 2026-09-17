package com.khoa.roommanagement.billing.bills.entity;

import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "bills")
public class Bill {

	@Id
	private UUID id;

	@Column(name = "contract_id", nullable = false)
	private UUID contractId;

	@Column(nullable = false)
	private String period;

	@Column(nullable = false)
	private long rentAmount;

	@Column(nullable = false)
	private long electricityUnitPrice;

	@Column(nullable = false)
	private long waterFee;

	@Column(nullable = false)
	private long serviceFee;

	@Column(nullable = false)
	private long oldMeterValue;

	@Column(nullable = false)
	private long newMeterValue;

	@Column(nullable = false)
	private long consumption;

	@Column(nullable = false)
	private long electricityAmount;

	@Column(nullable = false)
	private long totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BillStatus status;

	@Column(nullable = false)
	private LocalDate dueDate;

	@Column(nullable = false)
	private Instant createdAt;

	protected Bill() {
	}

	public static Bill createFrom(RentalContract contract, long currentMeterValue, long previousMeterValue, String period) {
		Bill bill = new Bill();
		bill.id = UUID.randomUUID();
		bill.contractId = contract.getId();
		bill.period = period;
		bill.rentAmount = contract.getRentAmount();
		bill.electricityUnitPrice = contract.getElectricityUnitPrice();
		bill.waterFee = contract.getWaterFee();
		bill.serviceFee = contract.getServiceFee();
		bill.oldMeterValue = previousMeterValue;
		bill.newMeterValue = currentMeterValue;
		bill.consumption = currentMeterValue - previousMeterValue;
		bill.electricityAmount = bill.consumption * contract.getElectricityUnitPrice();
		bill.totalAmount = bill.rentAmount + bill.electricityAmount + bill.waterFee + bill.serviceFee;
		bill.status = BillStatus.PENDING;
		bill.dueDate = calculateDueDate(period, contract.getPaymentDueDay());
		bill.createdAt = Instant.now();
		return bill;
	}

	private static LocalDate calculateDueDate(String period, int paymentDueDay) {
		YearMonth yearMonth = YearMonth.parse(period);
		return LocalDate.of(yearMonth.getYear(), yearMonth.getMonthValue(), paymentDueDay);
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

	public long getOldMeterValue() {
		return oldMeterValue;
	}

	public long getNewMeterValue() {
		return newMeterValue;
	}

	public long getConsumption() {
		return consumption;
	}

	public long getElectricityAmount() {
		return electricityAmount;
	}

	public long getTotalAmount() {
		return totalAmount;
	}

	public BillStatus getStatus() {
		return status;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean isPending() {
		return status == BillStatus.PENDING;
	}

	public void setStatus(BillStatus status) {
		this.status = status;
	}
}
