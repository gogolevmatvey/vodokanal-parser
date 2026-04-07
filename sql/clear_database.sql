-- ==========================================
-- Полная очистка базы данных vodokanal-db
-- Обнуление данных + сброс AUTO_INCREMENT (SERIAL)
-- ==========================================
-- Запуск:
-- psql -h localhost -p 5433 -U postgres -d vodokanal-db -f clear_database.sql
-- ==========================================

BEGIN;

-- Порядок: от дочерних к родительским
-- RESTART IDENTITY — сбрасывает счётчики ID (SERIAL) на 1

TRUNCATE TABLE meter_charges     RESTART IDENTITY CASCADE;
TRUNCATE TABLE billing_periods   RESTART IDENTITY CASCADE;
TRUNCATE TABLE accounts          RESTART IDENTITY CASCADE;
TRUNCATE TABLE apartments        RESTART IDENTITY CASCADE;
TRUNCATE TABLE houses            RESTART IDENTITY CASCADE;
TRUNCATE TABLE streets           RESTART IDENTITY CASCADE;
TRUNCATE TABLE localities        RESTART IDENTITY CASCADE;

COMMIT;

-- Проверка: все таблицы должны показать 0 строк
SELECT 'localities' AS table_name, COUNT(*) AS rows FROM localities
UNION ALL SELECT 'streets', COUNT(*) FROM streets
UNION ALL SELECT 'houses', COUNT(*) FROM houses
UNION ALL SELECT 'apartments', COUNT(*) FROM apartments
UNION ALL SELECT 'accounts', COUNT(*) FROM accounts
UNION ALL SELECT 'billing_periods', COUNT(*) FROM billing_periods
UNION ALL SELECT 'meter_charges', COUNT(*) FROM meter_charges
ORDER BY table_name;
