package com.khoa.roommanagement.billing.bills.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khoa.roommanagement.billing.bills.dto.CreateBillCommand;
import com.khoa.roommanagement.billing.bills.entity.Bill;
import com.khoa.roommanagement.billing.bills.exception.DuplicateBillException;
import com.khoa.roommanagement.billing.bills.exception.PreviousReadingNotFoundException;
import com.khoa.roommanagement.billing.bills.exception.ReadingNotFoundException;
import com.khoa.roommanagement.billing.bills.repository.BillRepository;
import com.khoa.roommanagement.billing.contracts.dto.CreateRentalContractCommand;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.exception.RentalContractNotFoundException;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import com.khoa.roommanagement.billing.electricity.entity.ElectricityReading;
import com.khoa.roommanagement.billing.electricity.repository.ElectricityReadingRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

	@Mock
	private BillRepository billRepository;

	@Mock
	private RentalContractRepository contractRepository;

	@Mock
	private ElectricityReadingRepository readingRepository;

	@InjectMocks
	private BillService billService;

	@Test
	void createsBillWithCorrectCalculation() {
		RentalContract contract = activeContract(3_500_000L, 4_000L, 100_000L, 150_000L);
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(billRepository.existsByContractIdAndPeriod(contractId, "2026-10")).thenReturn(false);

		ElectricityReading currentReading = ElectricityReading.record(contractId, "2026-10", 1200L);
		ElectricityReading previousReading = ElectricityReading.record(contractId, "2026-09", 1000L);
		when(readingRepository.findByContractIdAndPeriod(contractId, "2026-10"))
			.thenReturn(Optional.of(currentReading));
		when(readingRepository.findTopByContractIdAndPeriodLessThanOrderByPeriodDesc(contractId, "2026-10"))
			.thenReturn(Optional.of(previousReading));
		when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Bill bill = billService.create(new CreateBillCommand("2026-10"));

		assertThat(bill.getConsumption()).isEqualTo(200L);
		assertThat(bill.getElectricityAmount()).isEqualTo(800_000L);
		assertThat(bill.getTotalAmount()).isEqualTo(4_550_000L);
		assertThat(bill.getRentAmount()).isEqualTo(3_500_000L);
		assertThat(bill.getWaterFee()).isEqualTo(100_000L);
		assertThat(bill.getServiceFee()).isEqualTo(150_000L);
		assertThat(bill.getOldMeterValue()).isEqualTo(1000L);
		assertThat(bill.getNewMeterValue()).isEqualTo(1200L);
		assertThat(bill.getStatus().name()).isEqualTo("PENDING");
		assertThat(bill.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 5));
		verify(billRepository).save(any());
	}

	@Test
	void createsBillWithZeroConsumption() {
		RentalContract contract = activeContract(3_500_000L, 4_000L, 100_000L, 150_000L);
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(billRepository.existsByContractIdAndPeriod(contractId, "2026-10")).thenReturn(false);

		ElectricityReading currentReading = ElectricityReading.record(contractId, "2026-10", 1000L);
		ElectricityReading previousReading = ElectricityReading.record(contractId, "2026-09", 1000L);
		when(readingRepository.findByContractIdAndPeriod(contractId, "2026-10"))
			.thenReturn(Optional.of(currentReading));
		when(readingRepository.findTopByContractIdAndPeriodLessThanOrderByPeriodDesc(contractId, "2026-10"))
			.thenReturn(Optional.of(previousReading));
		when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Bill bill = billService.create(new CreateBillCommand("2026-10"));

		assertThat(bill.getConsumption()).isZero();
		assertThat(bill.getElectricityAmount()).isZero();
		assertThat(bill.getTotalAmount()).isEqualTo(3_750_000L);
	}

	@Test
	void snapshotsContractPrices() {
		RentalContract contract = activeContract(3_500_000L, 4_000L, 100_000L, 150_000L);
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(billRepository.existsByContractIdAndPeriod(contractId, "2026-10")).thenReturn(false);

		ElectricityReading currentReading = ElectricityReading.record(contractId, "2026-10", 1200L);
		ElectricityReading previousReading = ElectricityReading.record(contractId, "2026-09", 1000L);
		when(readingRepository.findByContractIdAndPeriod(contractId, "2026-10"))
			.thenReturn(Optional.of(currentReading));
		when(readingRepository.findTopByContractIdAndPeriodLessThanOrderByPeriodDesc(contractId, "2026-10"))
			.thenReturn(Optional.of(previousReading));
		when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Bill bill = billService.create(new CreateBillCommand("2026-10"));

		assertThat(bill.getRentAmount()).isEqualTo(contract.getRentAmount());
		assertThat(bill.getElectricityUnitPrice()).isEqualTo(contract.getElectricityUnitPrice());
		assertThat(bill.getWaterFee()).isEqualTo(contract.getWaterFee());
		assertThat(bill.getServiceFee()).isEqualTo(contract.getServiceFee());
	}

	@Test
	void rejectsDuplicateBill() {
		RentalContract contract = activeContract();
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(billRepository.existsByContractIdAndPeriod(contractId, "2026-10")).thenReturn(true);

		assertThatThrownBy(() -> billService.create(new CreateBillCommand("2026-10")))
			.isInstanceOf(DuplicateBillException.class);

		verify(billRepository, never()).save(any());
	}

	@Test
	void rejectsWhenNoReadingForPeriod() {
		RentalContract contract = activeContract();
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(billRepository.existsByContractIdAndPeriod(contractId, "2026-10")).thenReturn(false);
		when(readingRepository.findByContractIdAndPeriod(contractId, "2026-10"))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> billService.create(new CreateBillCommand("2026-10")))
			.isInstanceOf(ReadingNotFoundException.class);

		verify(billRepository, never()).save(any());
	}

	@Test
	void rejectsWhenNoPreviousReading() {
		RentalContract contract = activeContract();
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(billRepository.existsByContractIdAndPeriod(contractId, "2026-09")).thenReturn(false);

		ElectricityReading currentReading = ElectricityReading.record(contractId, "2026-09", 1200L);
		when(readingRepository.findByContractIdAndPeriod(contractId, "2026-09"))
			.thenReturn(Optional.of(currentReading));
		when(readingRepository.findTopByContractIdAndPeriodLessThanOrderByPeriodDesc(contractId, "2026-09"))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> billService.create(new CreateBillCommand("2026-09")))
			.isInstanceOf(PreviousReadingNotFoundException.class);

		verify(billRepository, never()).save(any());
	}

	@Test
	void rejectsWhenNoActiveContract() {
		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> billService.create(new CreateBillCommand("2026-10")))
			.isInstanceOf(RentalContractNotFoundException.class);

		verify(billRepository, never()).save(any());
	}

	@Test
	void calculatesDueDateFromPaymentDueDay() {
		RentalContract contract = activeContractWithDueDay(15);
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(billRepository.existsByContractIdAndPeriod(contractId, "2026-10")).thenReturn(false);

		ElectricityReading currentReading = ElectricityReading.record(contractId, "2026-10", 1200L);
		ElectricityReading previousReading = ElectricityReading.record(contractId, "2026-09", 1000L);
		when(readingRepository.findByContractIdAndPeriod(contractId, "2026-10"))
			.thenReturn(Optional.of(currentReading));
		when(readingRepository.findTopByContractIdAndPeriodLessThanOrderByPeriodDesc(contractId, "2026-10"))
			.thenReturn(Optional.of(previousReading));
		when(billRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Bill bill = billService.create(new CreateBillCommand("2026-10"));

		assertThat(bill.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 15));
	}

	private RentalContract activeContract() {
		return activeContract(3_500_000L, 4_000L, 100_000L, 150_000L);
	}

	private RentalContract activeContract(long rentAmount, long electricityPrice, long waterFee, long serviceFee) {
		return RentalContract.createActive(
			new CreateRentalContractCommand(
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2027, 8, 31),
				5,
				rentAmount,
				electricityPrice,
				waterFee,
				serviceFee
			)
		);
	}

	private RentalContract activeContractWithDueDay(int dueDay) {
		return RentalContract.createActive(
			new CreateRentalContractCommand(
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2027, 8, 31),
				dueDay,
				3_500_000L,
				4_000L,
				100_000L,
				150_000L
			)
		);
	}
}
