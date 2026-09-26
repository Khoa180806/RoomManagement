package com.khoa.roommanagement.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerAccountRepository extends JpaRepository<OwnerAccount, UUID> {

	Optional<OwnerAccount> findFirstBy();
}
