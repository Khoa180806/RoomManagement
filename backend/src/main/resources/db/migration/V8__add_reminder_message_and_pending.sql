-- Lưu snapshot nội dung tin nhắn để retry qua ngày không cần dựng lại,
-- và cho phép trạng thái PENDING cho reminder chưa được thử lần nào.
ALTER TABLE reminders ADD COLUMN message TEXT;

ALTER TABLE reminders DROP CONSTRAINT chk_reminders_status;
ALTER TABLE reminders ADD CONSTRAINT chk_reminders_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'));
