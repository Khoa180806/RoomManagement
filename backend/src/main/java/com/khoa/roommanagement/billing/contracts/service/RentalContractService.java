package com.khoa.roommanagement.billing.contracts.service;

import com.khoa.roommanagement.billing.contracts.dto.CreateRentalContractCommand;
import com.khoa.roommanagement.billing.contracts.exception.ActiveRentalContractAlreadyExistsException;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.exception.RentalContractNotFoundException;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RentalContractService {

	private final RentalContractRepository rentalContractRepository;

	public RentalContractService(RentalContractRepository rentalContractRepository) {
		this.rentalContractRepository = rentalContractRepository;
	}

	@Transactional
	public RentalContract create(CreateRentalContractCommand command) {
		if (rentalContractRepository.existsByStatus(RentalContractStatus.ACTIVE)) {
			throw new ActiveRentalContractAlreadyExistsException();
		}

		return rentalContractRepository.save(RentalContract.createActive(command));
	}

	@Transactional(readOnly = true)
	public RentalContract getActive() {
		return rentalContractRepository.findByStatus(RentalContractStatus.ACTIVE)
			.orElseThrow(RentalContractNotFoundException::new);
	}
}
