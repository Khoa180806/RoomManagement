package com.khoa.roommanagement.billing.electricity.repository;

import com.khoa.roommanagement.billing.electricity.entity.ElectricityReading;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectricityReadingRepository extends JpaRepository<ElectricityReading, UUID> {

	Optional<ElectricityReading> findByContractIdAndPeriod(UUID contractId, String period);

	List<ElectricityReading> findByContractIdOrderByPeriodDesc(UUID contractId);

	boolean existsByContractIdAndPeriod(UUID contractId, String period);

	Optional<ElectricityReading> findTopByContractIdAndPeriodLessThanOrderByPeriodDesc(UUID contractId, String period);
}
