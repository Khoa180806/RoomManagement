-- Bảng cấu hình nhắc (singleton) và lịch sử gửi nhắc
CREATE TABLE reminder_settings (
    id BIGINT PRIMARY KEY,
    bill_reminders_enabled BOOLEAN NOT NULL,
    bill_reminder_days_before VARCHAR(64) NOT NULL,
    overdue_reminders_enabled BOOLEAN NOT NULL,
    overdue_reminder_days VARCHAR(64) NOT NULL,
    contract_reminders_enabled BOOLEAN NOT NULL,
    contract_reminder_days_before VARCHAR(64) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE reminders (
    id UUID PRIMARY KEY,
    reminder_type VARCHAR(32) NOT NULL,
    reference_id UUID NOT NULL,
    target_date DATE NOT NULL,
    channel VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Idempotent theo khóa: loại nhắc + đối tượng + ngày dự kiến gửi + kênh
    CONSTRAINT uq_reminders_dedupe UNIQUE (reminder_type, reference_id, target_date, channel),
    CONSTRAINT chk_reminders_status CHECK (status IN ('SENT', 'FAILED'))
);

CREATE INDEX idx_reminders_status ON reminders (status);
CREATE INDEX idx_reminders_created_at ON reminders (created_at);
