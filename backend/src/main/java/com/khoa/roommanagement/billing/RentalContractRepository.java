package com.khoa.roommanagement.billing;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentalContractRepository extends JpaRepository<RentalContract, UUID> {

	boolean existsByStatus(RentalContractStatus status);
}
