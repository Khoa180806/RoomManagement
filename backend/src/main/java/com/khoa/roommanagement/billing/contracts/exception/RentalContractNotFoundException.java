package com.khoa.roommanagement.billing.contracts.exception;

public class RentalContractNotFoundException extends RuntimeException {

	public RentalContractNotFoundException() {
		super("Chưa có hợp đồng đang hiệu lực.");
	}
}
