package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Менеджер подключений к базе данных PostgreSQL.
 * Использует HikariCP для пула соединений.
 */
public class DatabaseManager implements AutoCloseable {

    private final HikariDataSource dataSource;

    // Параметры подключения
    private static final String DB_HOST = "localhost";
    private static final int DB_PORT = 5433;
    private static final String DB_NAME = "vodokanal-db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "postgres";

    public DatabaseManager() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", DB_HOST, DB_PORT, DB_NAME));
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);

        // Настройки пула
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Дополнительные настройки PostgreSQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Получение подключения из пула.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Инициализация базы данных - создание таблиц.
     */
    public void initializeDatabase() {
        System.out.println("Инициализация базы данных...");
        try (Connection conn = getConnection();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(
                             getClass().getClassLoader().getResourceAsStream("create_database.sql"),
                             StandardCharsets.UTF_8
                     )
             )) {

            if (reader == null) {
                // Файл не найден в resources, пробуем создать таблицы напрямую
                createTablesDirectly(conn);
                return;
            }

            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sqlBuilder.append(line).append("\n");
            }

            String[] sqlStatements = sqlBuilder.toString().split(";");
            for (String sql : sqlStatements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            System.out.println("База данных успешно инициализирована.");

        } catch (IOException e) {
            System.err.println("Файл create_database.sql не найден. Создаю таблицы напрямую...");
            try (Connection conn = getConnection()) {
                createTablesDirectly(conn);
            } catch (SQLException ex) {
                System.err.println("Ошибка создания таблиц: " + ex.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Ошибка инициализации БД: " + e.getMessage());
        }
    }

    /**
     * Создание таблиц напрямую (если файл SQL не найден).
     */
    private void createTablesDirectly(Connection conn) throws SQLException {
        String[] tables = {
            "CREATE TABLE IF NOT EXISTS localities (" +
            "    id SERIAL PRIMARY KEY," +
            "    name VARCHAR(255) NOT NULL UNIQUE" +
            ")",

            "CREATE INDEX IF NOT EXISTS idx_localities_name ON localities(name)",

            "CREATE TABLE IF NOT EXISTS streets (" +
            "    id SERIAL PRIMARY KEY," +
            "    locality_id INTEGER NOT NULL REFERENCES localities(id) ON DELETE CASCADE," +
            "    name VARCHAR(255) NOT NULL," +
            "    UNIQUE(locality_id, name)" +
            ")",

            "CREATE INDEX IF NOT EXISTS idx_streets_locality ON streets(locality_id)",
            "CREATE INDEX IF NOT EXISTS idx_streets_name ON streets(name)",

            "CREATE TABLE IF NOT EXISTS houses (" +
            "    id SERIAL PRIMARY KEY," +
            "    street_id INTEGER NOT NULL REFERENCES streets(id) ON DELETE CASCADE," +
            "    number VARCHAR(20) NOT NULL," +
            "    UNIQUE(street_id, number)" +
            ")",

            "CREATE INDEX IF NOT EXISTS idx_houses_street ON houses(street_id)",
            "CREATE INDEX IF NOT EXISTS idx_houses_number ON houses(number)",

            "CREATE TABLE IF NOT EXISTS apartments (" +
            "    id SERIAL PRIMARY KEY," +
            "    house_id INTEGER NOT NULL REFERENCES houses(id) ON DELETE CASCADE," +
            "    number VARCHAR(20) NOT NULL," +
            "    UNIQUE(house_id, number)" +
            ")",

            "CREATE INDEX IF NOT EXISTS idx_apartments_house ON apartments(house_id)",
            "CREATE INDEX IF NOT EXISTS idx_apartments_number ON apartments(number)",

            "CREATE TABLE IF NOT EXISTS accounts (" +
            "    id SERIAL PRIMARY KEY," +
            "    account_number VARCHAR(20) NOT NULL UNIQUE," +
            "    payer_name VARCHAR(255) NOT NULL," +
            "    apartment_id INTEGER REFERENCES apartments(id) ON DELETE SET NULL" +
            ")",

            "CREATE INDEX IF NOT EXISTS idx_accounts_number ON accounts(account_number)",
            "CREATE INDEX IF NOT EXISTS idx_accounts_apartment ON accounts(apartment_id)",
            "CREATE INDEX IF NOT EXISTS idx_accounts_payer ON accounts(payer_name)",

            "CREATE TABLE IF NOT EXISTS billing_periods (" +
            "    id SERIAL PRIMARY KEY," +
            "    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE," +
            "    period VARCHAR(10) NOT NULL," +
            "    total_amount DECIMAL(12,2) NOT NULL," +
            "    UNIQUE(account_id, period)" +
            ")",

            "CREATE INDEX IF NOT EXISTS idx_billing_account ON billing_periods(account_id)",
            "CREATE INDEX IF NOT EXISTS idx_billing_period ON billing_periods(period)",

            "CREATE TABLE IF NOT EXISTS meter_charges (" +
            "    id SERIAL PRIMARY KEY," +
            "    billing_period_id INTEGER NOT NULL REFERENCES billing_periods(id) ON DELETE CASCADE," +
            "    meter_name VARCHAR(255)," +
            "    reading DECIMAL(12,4)," +
            "    amount DECIMAL(12,2)" +
            ")",

            "CREATE INDEX IF NOT EXISTS idx_meter_charges_billing ON meter_charges(billing_period_id)",
            "CREATE INDEX IF NOT EXISTS idx_meter_charges_meter ON meter_charges(meter_name)"
        };

        for (String sql : tables) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
        System.out.println("База данных успешно инициализирована (таблицы созданы).");
    }

    /**
     * Закрытие пула соединений.
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Пул соединений закрыт.");
        }
    }

    /**
     * Проверка подключения к БД.
     */
    public boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
