package com.khoa.roommanagement.billing.electricity.dto;

public record CreateReadingCommand(
	String period,
	long meterValue
) {
}
