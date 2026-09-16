package com.khoa.roommanagement.billing.bills.repository;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, UUID> {

	Optional<Bill> findByContractIdAndPeriod(UUID contractId, String period);

	boolean existsByContractIdAndPeriod(UUID contractId, String period);

	Page<Bill> findByContractIdOrderByPeriodDesc(UUID contractId, Pageable pageable);
}
