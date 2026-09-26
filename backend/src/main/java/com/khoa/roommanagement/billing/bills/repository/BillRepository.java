package com.khoa.roommanagement.billing.bills.repository;

import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.entity.BillStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, UUID> {

	Optional<Bill> findByContractIdAndPeriod(UUID contractId, String period);

	boolean existsByContractIdAndPeriod(UUID contractId, String period);

	Page<Bill> findByContractIdOrderByPeriodDesc(UUID contractId, Pageable pageable);

	Page<Bill> findByContractIdAndPeriodOrderByPeriodDesc(UUID contractId, String period, Pageable pageable);

	List<Bill> findByStatusIn(Collection<BillStatus> statuses);
}
