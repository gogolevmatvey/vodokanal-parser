-- Скрипт обновления базы данных
-- Изменяет структуру meter_charges для правильной привязки к лицевым счетам
-- Выполните этот скрипт в pgAdmin

-- 1. Удаляем зависимые представления
DROP VIEW IF EXISTS v_search_addresses CASCADE;
DROP VIEW IF EXISTS v_full_accounts CASCADE;

-- 2. Удаляем старую таблицу meter_charges (данные будут потеряны!)
-- Если нужно сохранить данные, сделайте COPY или создайте временную таблицу
DROP TABLE IF EXISTS meter_charges CASCADE;

-- 3. Создаем новую таблицу meter_charges с правильной структурой
-- Привязка к account_id + period (а не к billing_period_id)
CREATE TABLE meter_charges (
    id SERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    period VARCHAR(10) NOT NULL,
    meter_name VARCHAR(255) NOT NULL,
    reading DECIMAL(12,4),
    amount DECIMAL(12,2),
    UNIQUE(account_id, period, meter_name)
);

CREATE INDEX idx_meter_charges_account ON meter_charges(account_id);
CREATE INDEX idx_meter_charges_period ON meter_charges(period);
CREATE INDEX idx_meter_charges_meter ON meter_charges(meter_name);

-- 4. Воссоздаем представления
CREATE VIEW v_search_addresses AS
SELECT 
    l.id AS locality_id,
    l.name AS locality_name,
    s.id AS street_id,
    s.name AS street_name,
    h.id AS house_id,
    h.number AS house_number,
    a.id AS apartment_id,
    a.number AS apartment_number
FROM localities l
JOIN streets s ON s.locality_id = l.id
JOIN houses h ON h.street_id = s.id
JOIN apartments a ON a.house_id = h.id;

CREATE VIEW v_full_accounts AS
SELECT 
    acc.account_number,
    acc.payer_name,
    l.name AS locality,
    s.name AS street,
    h.number AS house,
    a.number AS apartment,
    bp.period,
    bp.total_amount,
    mc.meter_name,
    mc.reading,
    mc.amount AS meter_amount
FROM accounts acc
LEFT JOIN apartments a ON a.id = acc.apartment_id
LEFT JOIN houses h ON h.id = a.house_id
LEFT JOIN streets s ON s.id = h.street_id
LEFT JOIN localities l ON l.id = s.locality_id
LEFT JOIN billing_periods bp ON bp.account_id = acc.id
LEFT JOIN meter_charges mc ON mc.account_id = acc.id AND mc.period = bp.period
ORDER BY acc.account_number, bp.period;

-- 5. Проверка
SELECT 'meter_charges' AS table_name, COUNT(*) AS row_count FROM meter_charges;
