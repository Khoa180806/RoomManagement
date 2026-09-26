package com.khoa.roommanagement.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình xác thực: SĐT chủ nhà seed khi khởi động lần đầu.
 * Không chứa mật khẩu hay bí mật khác — OTP/TOTP sinh động.
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
    String ownerPhone
) {
}
