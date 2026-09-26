package com.khoa.roommanagement.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;

/**
 * Integration test luồng đăng nhập TOTP và chặn 401 cho API được bảo vệ.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

	private static final String TOTP_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"; // "12345678901234567890"

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private OwnerAccountRepository ownerAccountRepository;

	@Autowired
	private org.springframework.security.web.FilterChainProxy springSecurityFilterChain;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.addFilters(springSecurityFilterChain)
			.build();
		ownerAccountRepository.deleteAll();
		ownerAccountRepository.save(OwnerAccount.create("912345678"));
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		ownerAccountRepository.deleteAll();
		SecurityContextHolder.clearContext();
	}

	@Test
	void protectedApiReturns401StructuredErrorWhenUnauthenticated() throws Exception {
		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/reminder-settings"))
			.andExpect(MockMvcResultMatchers.status().isUnauthorized())
			.andExpect(MockMvcResultMatchers.jsonPath("$.error.code").value("UNAUTHORIZED"))
			.andReturn();

		assertThat(result.getResponse().getContentAsString()).contains("Bạn cần đăng nhập");
	}

	@Test
	void healthEndpointStaysPublic() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
			.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	void totpLoginGrantsAccessToProtectedApi() throws Exception {
		OwnerAccount owner = ownerAccountRepository.findFirstBy().orElseThrow();
		owner.setTotpSecret(TOTP_SECRET);
		ownerAccountRepository.save(owner);

		String code = Totp.codeAt(TOTP_SECRET, Instant.now());
		MvcResult loginResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/verify-totp")
				.contentType("application/json")
				.content("{\"code\": \"%s\"}".formatted(code)))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.jsonPath("$.authenticated").value(true))
			.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
		assertThat(session).isNotNull();

		mockMvc.perform(MockMvcRequestBuilders.get("/api/reminder-settings").session(session))
			.andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	void otpRequestWithWrongPhoneReturns422() throws Exception {
		// Owner tồn tại (SĐT 912345678) — gửi SĐT khác phải bị chặn trước khi đụng Telegram
		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/request-otp")
				.contentType("application/json")
				.content("{\"phone\": \"0999999999\"}"))
			.andExpect(MockMvcResultMatchers.status().isUnprocessableEntity())
			.andExpect(MockMvcResultMatchers.jsonPath("$.error.code").value("PHONE_MISMATCH"));
	}

	@Test
	void meReturns401WhenUnauthenticated() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/me"))
			.andExpect(MockMvcResultMatchers.status().isUnauthorized());
	}
}
