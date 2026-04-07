-- Создание базы данных vodokanal-bd
-- Выполняется один раз при первом запуске

-- Таблица населенных пунктов
CREATE TABLE IF NOT EXISTS localities (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_localities_name ON localities(name);

-- Таблица улиц
CREATE TABLE IF NOT EXISTS streets (
    id SERIAL PRIMARY KEY,
    locality_id INTEGER NOT NULL REFERENCES localities(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    UNIQUE(locality_id, name)
);

CREATE INDEX IF NOT EXISTS idx_streets_locality ON streets(locality_id);
CREATE INDEX IF NOT EXISTS idx_streets_name ON streets(name);

-- Таблица домов
CREATE TABLE IF NOT EXISTS houses (
    id SERIAL PRIMARY KEY,
    street_id INTEGER NOT NULL REFERENCES streets(id) ON DELETE CASCADE,
    number VARCHAR(255) NOT NULL,
    UNIQUE(street_id, number)
);

CREATE INDEX IF NOT EXISTS idx_houses_street ON houses(street_id);
CREATE INDEX IF NOT EXISTS idx_houses_number ON houses(number);

-- Таблица квартир/помещений
CREATE TABLE IF NOT EXISTS apartments (
    id SERIAL PRIMARY KEY,
    house_id INTEGER NOT NULL REFERENCES houses(id) ON DELETE CASCADE,
    number VARCHAR(255) NOT NULL,
    UNIQUE(house_id, number)
);

CREATE INDEX IF NOT EXISTS idx_apartments_house ON apartments(house_id);
CREATE INDEX IF NOT EXISTS idx_apartments_number ON apartments(number);

-- Таблица лицевых счетов
CREATE TABLE IF NOT EXISTS accounts (
    id SERIAL PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    payer_name VARCHAR(255) NOT NULL,
    apartment_id INTEGER REFERENCES apartments(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_accounts_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_apartment ON accounts(apartment_id);
CREATE INDEX IF NOT EXISTS idx_accounts_payer ON accounts(payer_name);

-- Таблица периодов начислений
CREATE TABLE IF NOT EXISTS billing_periods (
    id SERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    period VARCHAR(10) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    UNIQUE(account_id, period)
);

CREATE INDEX IF NOT EXISTS idx_billing_account ON billing_periods(account_id);
CREATE INDEX IF NOT EXISTS idx_billing_period ON billing_periods(period);

-- Таблица начислений по приборам учета
-- Привязана к лицевому счету + период (а не к billing_period_id)
-- Это позволяет хранить историю показаний и избегать дублирования
CREATE TABLE IF NOT EXISTS meter_charges (
    id SERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    period VARCHAR(10) NOT NULL,
    meter_name VARCHAR(255) NOT NULL,
    reading DECIMAL(12,4),
    amount DECIMAL(12,2),
    UNIQUE(account_id, period, meter_name)
);

CREATE INDEX IF NOT EXISTS idx_meter_charges_account ON meter_charges(account_id);
CREATE INDEX IF NOT EXISTS idx_meter_charges_period ON meter_charges(period);
CREATE INDEX IF NOT EXISTS idx_meter_charges_meter ON meter_charges(meter_name);