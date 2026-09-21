-- Lưu hash nội dung của yêu cầu thanh toán gắn với idempotency key
ALTER TABLE payments ADD COLUMN payload_hash VARCHAR(64);
