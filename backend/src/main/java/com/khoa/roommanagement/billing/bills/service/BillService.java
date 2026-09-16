package com.khoa.roommanagement.billing.bills.service;

import com.khoa.roommanagement.billing.bills.dto.CreateBillCommand;
import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.exception.BillNotFoundException;
import com.khoa.roommanagement.billing.bills.exception.DuplicateBillException;
import com.khoa.roommanagement.billing.bills.exception.PreviousReadingNotFoundException;
import com.khoa.roommanagement.billing.bills.exception.ReadingNotFoundException;
import com.khoa.roommanagement.billing.bills.repository.BillRepository;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.exception.ContractTerminatedException;
import com.khoa.roommanagement.billing.contracts.exception.RentalContractNotFoundException;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import com.khoa.roommanagement.billing.electricity.entity.ElectricityReading;
import com.khoa.roommanagement.billing.electricity.repository.ElectricityReadingRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillService {

	private static final int MAX_PAGE_SIZE = 100;

	private final BillRepository billRepository;
	private final RentalContractRepository contractRepository;
	private final ElectricityReadingRepository readingRepository;

	public BillService(
		BillRepository billRepository,
		RentalContractRepository contractRepository,
		ElectricityReadingRepository readingRepository
	) {
		this.billRepository = billRepository;
		this.contractRepository = contractRepository;
		this.readingRepository = readingRepository;
	}

	@Transactional
	public Bill create(CreateBillCommand command) {
		RentalContract contract = getActiveContract();
		UUID contractId = contract.getId();
		String period = command.period();

		if (billRepository.existsByContractIdAndPeriod(contractId, period)) {
			throw new DuplicateBillException(period);
		}

		ElectricityReading currentReading = readingRepository
			.findByContractIdAndPeriod(contractId, period)
			.orElseThrow(() -> new ReadingNotFoundException(period));

		ElectricityReading previousReading = readingRepository
			.findTopByContractIdAndPeriodLessThanOrderByPeriodDesc(contractId, period)
			.orElseThrow(() -> new PreviousReadingNotFoundException(period));

		Bill bill = Bill.createFrom(contract, currentReading.getMeterValue(), previousReading.getMeterValue(), period);
		return billRepository.save(bill);
	}

	@Transactional(readOnly = true)
	public Bill getById(UUID id) {
		return billRepository.findById(id)
			.orElseThrow(() -> new BillNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public Page<Bill> getBills(int page, int size, String period) {
		int safeSize = Math.min(size, MAX_PAGE_SIZE);
		RentalContract contract = getActiveContract();
		
		if (period != null && !period.isEmpty()) {
			return billRepository.findByContractIdAndPeriodOrderByPeriodDesc(
				contract.getId(),
				period,
				PageRequest.of(page, safeSize)
			);
		}
		
		return billRepository.findByContractIdOrderByPeriodDesc(
			contract.getId(),
			PageRequest.of(page, safeSize)
		);
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
