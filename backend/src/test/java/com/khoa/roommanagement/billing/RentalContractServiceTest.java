package com.khoa.roommanagement.billing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RentalContractServiceTest {

	@Mock
	private RentalContractRepository rentalContractRepository;

	@InjectMocks
	private RentalContractService rentalContractService;

	@Test
	void rejectsCreatingAnotherActiveContract() {
		when(rentalContractRepository.existsByStatus(RentalContractStatus.ACTIVE)).thenReturn(true);

		assertThatThrownBy(() -> rentalContractService.create(validCommand()))
			.isInstanceOf(ActiveRentalContractAlreadyExistsException.class);

		verify(rentalContractRepository, never()).save(any());
	}

	private CreateRentalContractCommand validCommand() {
		return new CreateRentalContractCommand(
			LocalDate.of(2026, 9, 1),
			LocalDate.of(2027, 8, 31),
			5,
			3_500_000L,
			4_000L,
			100_000L,
			150_000L
		);
	}
}
