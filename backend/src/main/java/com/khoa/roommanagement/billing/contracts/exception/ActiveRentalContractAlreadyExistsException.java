package com.khoa.roommanagement.billing.contracts.exception;

public class ActiveRentalContractAlreadyExistsException extends RuntimeException {

	public ActiveRentalContractAlreadyExistsException() {
		super("Đã có hợp đồng đang hiệu lực.");
	}
}
