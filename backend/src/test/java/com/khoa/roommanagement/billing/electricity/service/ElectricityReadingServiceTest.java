package com.khoa.roommanagement.billing.electricity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khoa.roommanagement.billing.contracts.dto.CreateRentalContractCommand;
import com.khoa.roommanagement.billing.contracts.entity.RentalContract;
import com.khoa.roommanagement.billing.contracts.entity.RentalContractStatus;
import com.khoa.roommanagement.billing.contracts.exception.RentalContractNotFoundException;
import com.khoa.roommanagement.billing.contracts.repository.RentalContractRepository;
import com.khoa.roommanagement.billing.electricity.dto.CreateReadingCommand;
import com.khoa.roommanagement.billing.electricity.entity.ElectricityReading;
import com.khoa.roommanagement.billing.electricity.exception.DuplicateReadingException;
import com.khoa.roommanagement.billing.electricity.exception.MeterValueDecreasedException;
import com.khoa.roommanagement.billing.electricity.exception.NonConsecutivePeriodException;
import com.khoa.roommanagement.billing.electricity.repository.ElectricityReadingRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElectricityReadingServiceTest {

	@Mock
	private ElectricityReadingRepository readingRepository;

	@Mock
	private RentalContractRepository contractRepository;

	@InjectMocks
	private ElectricityReadingService readingService;

	@Test
	void recordsReadingSuccessfullyWhenNoPreviousReading() {
		RentalContract contract = activeContract();
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(readingRepository.existsByContractIdAndPeriod(contractId, "2026-09")).thenReturn(false);
		when(readingRepository.findByContractIdOrderByPeriodDesc(contractId)).thenReturn(List.of());
		when(readingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ElectricityReading result = readingService.record(new CreateReadingCommand("2026-09", 1200L));

		assertThat(result.getMeterValue()).isEqualTo(1200L);
		assertThat(result.getPeriod()).isEqualTo("2026-09");
		verify(readingRepository).save(any());
	}

	@Test
	void recordsReadingSuccessfullyWhenMeterValueIsHigher() {
		RentalContract contract = activeContract();
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(readingRepository.existsByContractIdAndPeriod(contractId, "2026-10")).thenReturn(false);
		ElectricityReading previous = ElectricityReading.record(contractId, "2026-09", 1000L);
		when(readingRepository.findByContractIdOrderByPeriodDesc(contractId)).thenReturn(List.of(previous));
		when(readingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ElectricityReading result = readingService.record(new CreateReadingCommand("2026-10", 1200L));

		assertThat(result.getMeterValue()).isEqualTo(1200L);
		verify(readingRepository).save(any());
	}

	@Test
	void rejectsDuplicatePeriod() {
		RentalContract contract = activeContract();
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(readingRepository.existsByContractIdAndPeriod(contractId, "2026-09")).thenReturn(true);

		assertThatThrownBy(() -> readingService.record(new CreateReadingCommand("2026-09", 1200L)))
			.isInstanceOf(DuplicateReadingException.class);

		verify(readingRepository, never()).save(any());
	}

	@Test
	void rejectsMeterValueDecrease() {
		RentalContract contract = activeContract();
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(readingRepository.existsByContractIdAndPeriod(contractId, "2026-10")).thenReturn(false);
		ElectricityReading previous = ElectricityReading.record(contractId, "2026-09", 1200L);
		when(readingRepository.findByContractIdOrderByPeriodDesc(contractId)).thenReturn(List.of(previous));

		assertThatThrownBy(() -> readingService.record(new CreateReadingCommand("2026-10", 1000L)))
			.isInstanceOf(MeterValueDecreasedException.class);

		verify(readingRepository, never()).save(any());
	}

	@Test
	void rejectsNonConsecutivePeriod() {
		RentalContract contract = activeContract();
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(readingRepository.existsByContractIdAndPeriod(contractId, "2026-12")).thenReturn(false);
		ElectricityReading previous = ElectricityReading.record(contractId, "2026-09", 1200L);
		when(readingRepository.findByContractIdOrderByPeriodDesc(contractId)).thenReturn(List.of(previous));

		assertThatThrownBy(() -> readingService.record(new CreateReadingCommand("2026-12", 1500L)))
			.isInstanceOf(NonConsecutivePeriodException.class);

		verify(readingRepository, never()).save(any());
	}

	@Test
	void rejectsWhenNoActiveContract() {
		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> readingService.record(new CreateReadingCommand("2026-09", 1200L)))
			.isInstanceOf(RentalContractNotFoundException.class);

		verify(readingRepository, never()).save(any());
	}

	@Test
	void allowsEqualMeterValue() {
		RentalContract contract = activeContract();
		UUID contractId = contract.getId();

		when(contractRepository.findByStatus(RentalContractStatus.ACTIVE))
			.thenReturn(Optional.of(contract));
		when(readingRepository.existsByContractIdAndPeriod(contractId, "2026-10")).thenReturn(false);
		ElectricityReading previous = ElectricityReading.record(contractId, "2026-09", 1200L);
		when(readingRepository.findByContractIdOrderByPeriodDesc(contractId)).thenReturn(List.of(previous));
		when(readingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ElectricityReading result = readingService.record(new CreateReadingCommand("2026-10", 1200L));

		assertThat(result.getMeterValue()).isEqualTo(1200L);
		verify(readingRepository).save(any());
	}

	private RentalContract activeContract() {
		return RentalContract.createActive(
			new CreateRentalContractCommand(
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2027, 8, 31),
				5,
				3_500_000L,
				4_000L,
				100_000L,
				150_000L
			)
		);
	}
}
