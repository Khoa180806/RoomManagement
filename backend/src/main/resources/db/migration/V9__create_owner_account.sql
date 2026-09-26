-- Tài khoản chủ nhà (một dòng duy nhất) cho đăng nhập SĐT + OTP/TOTP
CREATE TABLE owner_account (
    id UUID PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    totp_secret TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
