-- Bảng thanh toán
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    bill_id UUID NOT NULL UNIQUE REFERENCES bills(id),
    paid_at TIMESTAMP WITH TIME ZONE NOT NULL,
    note TEXT,
    on_time BOOLEAN NOT NULL,
    idempotency_key VARCHAR(255) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_payments_paid_at_not_future CHECK (paid_at <= NOW())
);

CREATE INDEX idx_payments_bill_id ON payments (bill_id);
CREATE INDEX idx_payments_idempotency_key ON payments (idempotency_key);
