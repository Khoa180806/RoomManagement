package com.khoa.roommanagement.billing.bills.exception;

import java.util.UUID;

public class BillNotFoundException extends RuntimeException {

	public BillNotFoundException(UUID id) {
		super("Không tìm thấy hóa đơn " + id + ".");
	}
}
