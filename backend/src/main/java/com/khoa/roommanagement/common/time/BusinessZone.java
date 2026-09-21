package com.khoa.roommanagement.common.time;

import java.time.ZoneId;

/**
 * Múi giờ nghiệp vụ cố định cho toàn bộ tính toán ngày đến hạn và trễ hạn.
 */
public final class BusinessZone {

	public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

	private BusinessZone() {
	}
}
