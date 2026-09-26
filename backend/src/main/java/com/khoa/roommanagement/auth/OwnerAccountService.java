package com.khoa.roommanagement.auth;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerAccountService {

	private final OwnerAccountRepository repository;
	private final AuthProperties properties;

	public OwnerAccountService(OwnerAccountRepository repository, AuthProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	/**
	 * Lấy tài khoản chủ nhà; nếu chưa có và env OWNER_PHONE được đặt thì seed.
	 * Có thể rỗng khi app mới cài và chưa cấu hình đăng nhập.
	 */
	@Transactional
	public Optional<OwnerAccount> getOwner() {
		return repository.findFirstBy()
			.or(() -> {
				if (properties.ownerPhone() == null || properties.ownerPhone().isBlank()) {
					return Optional.empty();
				}
				return Optional.of(repository.save(OwnerAccount.create(Phone.normal(properties.ownerPhone()))));
			});
	}

	@Transactional
	public OwnerAccount requireOwner() {
		return getOwner().orElseThrow(AuthNotConfiguredException::new);
	}

	@Transactional
	public void updatePhone(OwnerAccount account, String phone) {
		account.updatePhone(phone);
		repository.save(account);
	}

	@Transactional
	public void setTotpSecret(OwnerAccount account, String secret) {
		account.setTotpSecret(secret);
		repository.save(account);
	}
}
