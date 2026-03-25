-- Скрипт для получения данных в десктопный интерфейс
-- Эти запросы можно использовать для заполнения выпадающих списков

-- ============================================================================
-- 1. Получить все населенные пункты (для первого выпадающего списка)
-- ============================================================================
SELECT id, name
FROM localities
ORDER BY name;

-- ============================================================================
-- 2. Получить улицы по выбранному населенному пункту
-- Параметр: :locality_id - ID выбранного населенного пункта
-- ============================================================================
SELECT id, name
FROM streets
WHERE locality_id = :locality_id
ORDER BY name;

-- ============================================================================
-- 3. Получить дома по выбранной улице
-- Параметр: :street_id - ID выбранной улицы
-- ============================================================================
SELECT id, number
FROM houses
WHERE street_id = :street_id
ORDER BY number;

-- ============================================================================
-- 4. Получить квартиры по выбранному дому
-- Параметр: :house_id - ID выбранного дома
-- ============================================================================
SELECT id, number
FROM apartments
WHERE house_id = :house_id
ORDER BY number;

-- ============================================================================
-- 5. Поиск лицевых счетов по адресу (для поиска)
-- Параметры: :locality_name, :street_name, :house_number, :apartment_number
-- ============================================================================
SELECT 
    acc.account_number,
    acc.payer_name,
    l.name AS locality,
    s.name AS street,
    h.number AS house,
    a.number AS apartment
FROM accounts acc
JOIN apartments a ON a.id = acc.apartment_id
JOIN houses h ON h.id = a.house_id
JOIN streets s ON s.id = h.street_id
JOIN localities l ON l.id = s.locality_id
WHERE 
    (:locality_name::varchar IS NULL OR l.name = :locality_name)
    AND (:street_name::varchar IS NULL OR s.name = :street_name)
    AND (:house_number::varchar IS NULL OR h.number = :house_number)
    AND (:apartment_number::varchar IS NULL OR a.number = :apartment_number)
ORDER BY acc.account_number;

-- ============================================================================
-- 6. Поиск лицевого счета по номеру
-- Параметр: :account_number - номер лицевого счета (можно с маской)
-- ============================================================================
SELECT 
    acc.account_number,
    acc.payer_name,
    l.name AS locality,
    s.name AS street,
    h.number AS house,
    a.number AS apartment
FROM accounts acc
LEFT JOIN apartments a ON a.id = acc.apartment_id
LEFT JOIN houses h ON h.id = a.house_id
LEFT JOIN streets s ON s.id = h.street_id
LEFT JOIN localities l ON l.id = s.locality_id
WHERE acc.account_number LIKE :account_number
ORDER BY acc.account_number;

-- ============================================================================
-- 7. Получить информацию о периодах начислений по лицевому счету
-- Параметр: :account_number - номер лицевого счета
-- ============================================================================
SELECT 
    bp.period,
    bp.total_amount,
    mc.meter_name,
    mc.reading,
    mc.amount AS meter_amount
FROM billing_periods bp
LEFT JOIN meter_charges mc ON mc.account_id = bp.account_id AND mc.period = bp.period
JOIN accounts acc ON acc.id = bp.account_id
WHERE acc.account_number = :account_number
ORDER BY bp.period DESC;

-- ============================================================================
-- 8. Статистика по базе данных
-- ============================================================================
SELECT 
    (SELECT COUNT(*) FROM localities) AS localities_count,
    (SELECT COUNT(*) FROM streets) AS streets_count,
    (SELECT COUNT(*) FROM houses) AS houses_count,
    (SELECT COUNT(*) FROM apartments) AS apartments_count,
    (SELECT COUNT(*) FROM accounts) AS accounts_count,
    (SELECT COUNT(*) FROM billing_periods) AS billing_periods_count,
    (SELECT COUNT(*) FROM meter_charges) AS meter_charges_count;
