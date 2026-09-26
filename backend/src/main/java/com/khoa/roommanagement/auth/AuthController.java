package com.khoa.roommanagement.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	public record RequestOtpRequest(@NotBlank String phone) {
	}

	public record VerifyOtpRequest(@NotBlank String phone, @Pattern(regexp = "\\d{6}", message = "Mã OTP gồm 6 chữ số") String code) {
	}

	public record VerifyTotpRequest(@NotBlank @Pattern(regexp = "\\d{6}", message = "Mã TOTP gồm 6 chữ số") String code) {
	}

	public record OtpIssuedResponse(boolean sent, long expiresInSeconds) {
	}

	public record AuthenticatedResponse(boolean authenticated) {
	}

	public record MeResponse(String phone, boolean totpEnabled) {
	}

	public record TotpSetupResponse(String otpauthUri) {
	}

	private final AuthService authService;
	private final OwnerAccountService accountService;
	private final SecurityContextRepository securityContextRepository;

	public AuthController(
		AuthService authService,
		OwnerAccountService accountService,
		SecurityContextRepository securityContextRepository
	) {
		this.authService = authService;
		this.accountService = accountService;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/request-otp")
	public OtpIssuedResponse requestOtp(@Valid @RequestBody RequestOtpRequest request) {
		var issued = authService.requestOtp(request.phone());
		return new OtpIssuedResponse(true, issued.expiresInSeconds());
	}

	@PostMapping("/verify-otp")
	public AuthenticatedResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		OwnerAccount owner = authService.verifyOtp(request.phone(), request.code());
		establishSession(owner, httpRequest, httpResponse);
		return new AuthenticatedResponse(true);
	}

	@PostMapping("/verify-totp")
	public AuthenticatedResponse verifyTotp(@Valid @RequestBody VerifyTotpRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		OwnerAccount owner = authService.verifyTotp(request.code());
		establishSession(owner, httpRequest, httpResponse);
		return new AuthenticatedResponse(true);
	}

	private void establishSession(OwnerAccount owner, HttpServletRequest request, HttpServletResponse response) {
		var authentication = new UsernamePasswordAuthenticationToken(
			owner.getPhone(), null, List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}

	@GetMapping("/me")
	public MeResponse me(Authentication authentication) {
		OwnerAccount owner = requireOwner(authentication);
		return new MeResponse(Phone.mask(owner.getPhone()), owner.isTotpEnabled());
	}

	@PostMapping("/totp/setup")
	public TotpSetupResponse setupTotp(Authentication authentication) {
		OwnerAccount owner = requireOwner(authentication);
		return new TotpSetupResponse(authService.beginTotpSetup(owner));
	}

	@PostMapping("/totp/enable")
	public AuthenticatedResponse enableTotp(@Valid @RequestBody VerifyTotpRequest request,
			Authentication authentication) {
		OwnerAccount owner = requireOwner(authentication);
		authService.confirmTotpSetup(owner, request.code());
		return new AuthenticatedResponse(true);
	}

	@PostMapping("/totp/disable")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void disableTotp(Authentication authentication) {
		OwnerAccount owner = requireOwner(authentication);
		authService.disableTotp(owner);
	}

	/** Anonymous token cũng có principal dạng String nên phải kiểm tra đúng loại token. */
	private OwnerAccount requireOwner(Authentication authentication) {
		if (!(authentication instanceof UsernamePasswordAuthenticationToken)
			|| !(authentication.getPrincipal() instanceof String phone)
			|| !authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_OWNER"))) {
			throw new UnauthenticatedException();
		}
		OwnerAccount owner = accountService.getOwner().orElseThrow(UnauthenticatedException::new);
		if (!owner.getPhone().equals(phone)) {
			throw new UnauthenticatedException();
		}
		return owner;
	}

	public static class UnauthenticatedException extends RuntimeException {

		public UnauthenticatedException() {
			super("Chưa đăng nhập.");
		}
	}
}
