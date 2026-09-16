-- Chỉ số điện theo kỳ
CREATE TABLE electricity_readings (
    id                UUID PRIMARY KEY,
    contract_id       UUID         NOT NULL REFERENCES rental_contracts (id),
    period            VARCHAR(7)   NOT NULL,
    meter_value       BIGINT       NOT NULL CHECK (meter_value >= 0),
    recorded_at       TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_electricity_readings_contract_period
        UNIQUE (contract_id, period),

    CONSTRAINT chk_electricity_readings_period_format
        CHECK (period ~ '^\d{4}-\d{2}$')
);

-- Hóa đơn theo kỳ (snapshot toàn bộ giá tại thời điểm tạo)
CREATE TABLE bills (
    id                     UUID PRIMARY KEY,
    contract_id            UUID         NOT NULL REFERENCES rental_contracts (id),
    period                 VARCHAR(7)   NOT NULL,
    rent_amount            BIGINT       NOT NULL CHECK (rent_amount >= 0),
    electricity_unit_price BIGINT       NOT NULL CHECK (electricity_unit_price >= 0),
    water_fee              BIGINT       NOT NULL CHECK (water_fee >= 0),
    service_fee            BIGINT       NOT NULL CHECK (service_fee >= 0),
    old_meter_value        BIGINT       NOT NULL CHECK (old_meter_value >= 0),
    new_meter_value        BIGINT       NOT NULL CHECK (new_meter_value >= 0),
    consumption            BIGINT       NOT NULL CHECK (consumption >= 0),
    electricity_amount     BIGINT       NOT NULL CHECK (electricity_amount >= 0),
    total_amount           BIGINT       NOT NULL CHECK (total_amount >= 0),
    status                 VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'PAID', 'OVERDUE')),
    due_date               DATE         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_bills_contract_period
        UNIQUE (contract_id, period),

    CONSTRAINT chk_bills_period_format
        CHECK (period ~ '^\d{4}-\d{2}$'),

    CONSTRAINT chk_bills_new_gte_old
        CHECK (new_meter_value >= old_meter_value),

    CONSTRAINT chk_bills_consumption
        CHECK (consumption = new_meter_value - old_meter_value),

    CONSTRAINT chk_bills_electricity_amount
        CHECK (electricity_amount = consumption * electricity_unit_price),

    CONSTRAINT chk_bills_total_amount
        CHECK (total_amount = rent_amount + electricity_amount + water_fee + service_fee)
);

CREATE INDEX idx_electricity_readings_contract ON electricity_readings (contract_id);
CREATE INDEX idx_bills_contract ON bills (contract_id);
