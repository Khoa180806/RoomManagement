CREATE TABLE rental_contracts (
    id UUID PRIMARY KEY,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    payment_due_day SMALLINT NOT NULL CHECK (payment_due_day BETWEEN 1 AND 28),
    rent_amount BIGINT NOT NULL CHECK (rent_amount >= 0),
    electricity_unit_price BIGINT NOT NULL CHECK (electricity_unit_price >= 0),
    water_fee BIGINT NOT NULL CHECK (water_fee >= 0),
    service_fee BIGINT NOT NULL CHECK (service_fee >= 0),
    status VARCHAR(50) NOT NULL,
    active_contract_marker BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (end_date > start_date),
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'TERMINATED', 'TERMINATED_FOR_NON_PAYMENT')),
    CHECK ((status = 'ACTIVE' AND active_contract_marker = TRUE)
        OR (status <> 'ACTIVE' AND active_contract_marker IS NULL))
);

CREATE UNIQUE INDEX uq_rental_contracts_active_marker
    ON rental_contracts (active_contract_marker);
