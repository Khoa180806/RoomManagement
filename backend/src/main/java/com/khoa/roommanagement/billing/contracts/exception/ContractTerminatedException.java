package com.khoa.roommanagement.billing.contracts.exception;

public class ContractTerminatedException extends RuntimeException {

	public ContractTerminatedException() {
		super("Hợp đồng đã bị hủy do không thanh toán. Không thể thực hiện thao tác này.");
	}
}
