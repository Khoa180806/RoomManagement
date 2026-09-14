package com.khoa.roommanagement.billing;

public class RentalContractNotFoundException extends RuntimeException {

	public RentalContractNotFoundException() {
		super("Chưa có hợp đồng đang hiệu lực.");
	}
}
