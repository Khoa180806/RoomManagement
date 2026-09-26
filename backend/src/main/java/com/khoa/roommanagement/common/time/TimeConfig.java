package com.khoa.roommanagement.common.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class TimeConfig {

	/**
	 * Clock nghiệp vụ theo múi giờ Việt Nam; test có thể thay bằng clock cố định.
	 */
	@Bean
	public Clock businessClock() {
		return Clock.system(BusinessZone.VIETNAM);
	}
}
