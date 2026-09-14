package com.khoa.roommanagement.billing;

public class ActiveRentalContractAlreadyExistsException extends RuntimeException {

	public ActiveRentalContractAlreadyExistsException() {
		super("Đã có hợp đồng đang hiệu lực.");
	}
}
