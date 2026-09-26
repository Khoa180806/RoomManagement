package com.khoa.roommanagement.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Trả 401 với định dạng lỗi có cấu trúc dùng chung thay vì redirect/HTML
 * mặc định của Spring Security.
 */
@Component
public class StructuredAuthEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authenticationException) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json;charset=UTF-8");
		response.getWriter().write(
			"""
			{"error":{"code":"UNAUTHORIZED","message":"Bạn cần đăng nhập để thực hiện thao tác này.","details":[]}}""");
	}
}
