package com.khoa.roommanagement.billing.electricity.service;

import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.exception.ContractTerminatedException;
import com.khoa.roommanagement.billing.contracts.exception.RentalContractNotFoundException;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import com.khoa.roommanagement.billing.electricity.dto.CreateReadingCommand;
import com.khoa.roommanagement.billing.electricity.entity.ElectricityReading;
import com.khoa.roommanagement.billing.electricity.exception.DuplicateReadingException;
import com.khoa.roommanagement.billing.electricity.exception.MeterValueDecreasedException;
import com.khoa.roommanagement.billing.electricity.exception.MeterValueExceedsNextException;
import com.khoa.roommanagement.billing.electricity.repository.ElectricityReadingRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElectricityReadingService {

	private final ElectricityReadingRepository readingRepository;
	private final RentalContractRepository contractRepository;

	public ElectricityReadingService(
		ElectricityReadingRepository readingRepository,
		RentalContractRepository contractRepository
	) {
		this.readingRepository = readingRepository;
		this.contractRepository = contractRepository;
	}

	@Transactional
	public ElectricityReading record(CreateReadingCommand command) {
		RentalContract contract = getActiveContract();
		UUID contractId = contract.getId();
		String period = command.period();

		if (readingRepository.existsByContractIdAndPeriod(contractId, period)) {
			throw new DuplicateReadingException(period);
		}

		// Tìm kỳ ngay TRƯỚC period này (theo thứ tự thời gian)
		Optional<ElectricityReading> previousReading = readingRepository
			.findTopByContractIdAndPeriodLessThanOrderByPeriodDesc(contractId, period);

		if (previousReading.isPresent() && command.meterValue() < previousReading.get().getMeterValue()) {
			throw new MeterValueDecreasedException(command.meterValue(), previousReading.get().getMeterValue());
		}

		// Tìm kỳ ngay SAU period này (theo thứ tự thời gian) — cho phép nhập bù kỳ cũ
		Optional<ElectricityReading> nextReading = readingRepository
			.findTopByContractIdAndPeriodGreaterThanOrderByPeriodAsc(contractId, period);

		if (nextReading.isPresent() && command.meterValue() > nextReading.get().getMeterValue()) {
			throw new MeterValueExceedsNextException(command.meterValue(), nextReading.get().getMeterValue());
		}

		return readingRepository.save(
			ElectricityReading.record(contractId, period, command.meterValue())
		);
	}

	@Transactional(readOnly = true)
	public List<ElectricityReading> getReadingsByActiveContract() {
		RentalContract contract = getActiveContract();
		return readingRepository.findByContractIdOrderByPeriodDesc(contract.getId());
	}

	private RentalContract getActiveContract() {
		return contractRepository.findByStatus(RentalContractStatus.ACTIVE)
			.orElseGet(() -> {
				if (contractRepository.existsByStatus(RentalContractStatus.TERMINATED_FOR_NON_PAYMENT)) {
					throw new ContractTerminatedException();
				}
				throw new RentalContractNotFoundException();
			});
	}
}
