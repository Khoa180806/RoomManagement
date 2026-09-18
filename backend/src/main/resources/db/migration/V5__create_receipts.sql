-- Bảng chứng từ thanh toán (một ảnh cho mỗi thanh toán trong MVP)
CREATE TABLE receipts (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE REFERENCES payments(id),
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_receipts_payment_id ON receipts (payment_id);
