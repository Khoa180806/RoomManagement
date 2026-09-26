package com.khoa.roommanagement.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthControllerTest.TestBeans.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private OwnerAccountService accountService;

	static class TestBeans {
		@org.springframework.context.annotation.Bean
		SecurityContextRepository securityContextRepository() {
			return new HttpSessionSecurityContextRepository();
		}
	}

	private void stubAuthenticatedOwner() {
		when(accountService.getOwner()).thenReturn(java.util.Optional.of(OwnerAccount.create("912345678")));
	}

	private UsernamePasswordAuthenticationToken ownerAuthentication() {
		return new UsernamePasswordAuthenticationToken(
			"912345678", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
	}

	/** MockMvc với addFilters=false không có userPrincipal — gắn trực tiếp vào request. */
	private org.springframework.test.web.servlet.request.RequestPostProcessor principal() {
		return request -> {
			request.setUserPrincipal(ownerAuthentication());
			return request;
		};
	}

	@org.junit.jupiter.api.AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void issuesOtpForMatchingPhone() throws Exception {
		when(authService.requestOtp("0912345678")).thenReturn(new AuthService.OtpIssued(300));

		mockMvc.perform(post("/api/auth/request-otp")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"phone": "0912345678"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.sent").value(true))
			.andExpect(jsonPath("$.expiresInSeconds").value(300));
	}

	@Test
	void rejectsRequestOtpWithBlankPhone() throws Exception {
		mockMvc.perform(post("/api/auth/request-otp")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"phone": ""}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void verifyOtpEstablishesSession() throws Exception {
		when(authService.verifyOtp("0912345678", "123456")).thenReturn(OwnerAccount.create("912345678"));

		mockMvc.perform(post("/api/auth/verify-otp")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"phone": "0912345678", "code": "123456"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true));
	}

	@Test
	void returns401StructuredErrorForMeWhenUnauthenticated() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void returnsMeForAuthenticatedOwner() throws Exception {
		stubAuthenticatedOwner();

		mockMvc.perform(get("/api/auth/me").with(principal()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.phone").value("912****678"))
			.andExpect(jsonPath("$.totpEnabled").value(false));
	}

	@Test
	void returnsTotpSetupUriForAuthenticatedOwner() throws Exception {
		stubAuthenticatedOwner();
		when(authService.beginTotpSetup(any(OwnerAccount.class)))
			.thenReturn("otpauth://totp/RoomManagement:912345678?secret=ABC234567DEF");

		mockMvc.perform(post("/api/auth/totp/setup").with(principal()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.otpauthUri").value(
				"otpauth://totp/RoomManagement:912345678?secret=ABC234567DEF"));
	}

	@Test
	void totpSetupRequiresAuthentication() throws Exception {
		when(accountService.getOwner()).thenReturn(java.util.Optional.empty());

		mockMvc.perform(post("/api/auth/totp/setup"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
	}

	@Test
	void otpIssuedResponseShapeIsStable() throws Exception {
		when(authService.requestOtp(anyString())).thenReturn(new AuthService.OtpIssued(300));

		String body = mockMvc.perform(post("/api/auth/request-otp")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"phone": "0912345678"}
					"""))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		org.assertj.core.api.Assertions.assertThat(body)
			.isEqualTo("{\"sent\":true,\"expiresInSeconds\":300}");
	}

	@Test
	void verifyTotpRejectsMalformedCode() throws Exception {
		mockMvc.perform(post("/api/auth/verify-totp")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"code": "abc"}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	void meNeverTouchesAuthService() throws Exception {
		stubAuthenticatedOwner();

		mockMvc.perform(get("/api/auth/me").with(principal()));

		Mockito.verifyNoInteractions(authService);
	}
}
