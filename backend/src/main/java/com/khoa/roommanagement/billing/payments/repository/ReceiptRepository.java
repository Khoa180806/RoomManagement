package com.khoa.roommanagement.billing.payments.repository;

import com.khoa.roommanagement.billing.payments.entity.Receipt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

	Optional<Receipt> findByPaymentId(UUID paymentId);
}
