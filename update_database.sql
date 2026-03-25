-- Скрипт обновления базы данных
-- Выполните этот скрипт в pgAdmin для исправления размеров полей

-- 1. Удаляем зависимые представления
DROP VIEW IF EXISTS v_search_addresses CASCADE;
DROP VIEW IF EXISTS v_full_accounts CASCADE;

-- 2. Увеличиваем размер поля number в таблице houses с 20 до 255 символов
-- Это необходимо для хранения длинных описаний домов (общежития, нежилые помещения и т.д.)
ALTER TABLE houses ALTER COLUMN number TYPE VARCHAR(255);

-- 3. Увеличиваем размер поля number в таблице apartments с 20 до 255 символов
-- (на случай, если ещё не было выполнено)
ALTER TABLE apartments ALTER COLUMN number TYPE VARCHAR(255);

-- 4. Воссоздаем представления
CREATE OR REPLACE VIEW v_search_addresses AS
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

CREATE OR REPLACE VIEW v_full_accounts AS
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

-- 5. Проверка результата
SELECT 
    (SELECT COUNT(*) FROM houses) AS houses_count,
    (SELECT COUNT(*) FROM apartments) AS apartments_count;
