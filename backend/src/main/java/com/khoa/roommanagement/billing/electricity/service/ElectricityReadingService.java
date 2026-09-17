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
import com.khoa.roommanagement.billing.electricity.exception.NonConsecutivePeriodException;
import com.khoa.roommanagement.billing.electricity.repository.ElectricityReadingRepository;
import java.time.YearMonth;
import java.util.List;
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

		List<ElectricityReading> existingReadings = readingRepository
			.findByContractIdOrderByPeriodDesc(contractId);

		if (!existingReadings.isEmpty()) {
			String lastPeriod = existingReadings.get(0).getPeriod();
			long previousMeterValue = existingReadings.get(0).getMeterValue();

			// Kiểm tra period mới phải là kỳ tiếp theo
			YearMonth lastYearMonth = YearMonth.parse(lastPeriod);
			YearMonth expectedNextPeriod = lastYearMonth.plusMonths(1);
			YearMonth requestedPeriod = YearMonth.parse(period);

			if (!requestedPeriod.equals(expectedNextPeriod)) {
				throw new NonConsecutivePeriodException(period, lastPeriod);
			}

			// Kiểm tra chỉ số không được giảm
			if (command.meterValue() < previousMeterValue) {
				throw new MeterValueDecreasedException(command.meterValue(), previousMeterValue);
			}
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
