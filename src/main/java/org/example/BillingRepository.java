package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с периодами начислений и приборами учета.
 */
public class BillingRepository {

    private final DatabaseManager dbManager;

    private PreparedStatement getBillingPeriodStmt;
    private PreparedStatement insertBillingPeriodStmt;
    private PreparedStatement getMeterChargeStmt;
    private PreparedStatement insertOrUpdateMeterChargeStmt;

    public BillingRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Инициализация prepared statements.
     */
    public void init() throws SQLException {
        Connection conn = dbManager.getConnection();

        getBillingPeriodStmt = conn.prepareStatement(
            "SELECT id, total_amount FROM billing_periods WHERE account_id = ? AND period = ?"
        );
        insertBillingPeriodStmt = conn.prepareStatement(
            "INSERT INTO billing_periods (account_id, period, total_amount) VALUES (?, ?, ?) " +
            "ON CONFLICT (account_id, period) DO UPDATE SET total_amount = EXCLUDED.total_amount " +
            "RETURNING id",
            PreparedStatement.RETURN_GENERATED_KEYS
        );
        getMeterChargeStmt = conn.prepareStatement(
            "SELECT id, reading, amount FROM meter_charges WHERE account_id = ? AND period = ? AND meter_name = ?"
        );
        insertOrUpdateMeterChargeStmt = conn.prepareStatement(
            "INSERT INTO meter_charges (account_id, period, meter_name, reading, amount) VALUES (?, ?, ?, ?, ?) " +
            "ON CONFLICT (account_id, period, meter_name) DO UPDATE SET reading = EXCLUDED.reading, amount = EXCLUDED.amount"
        );
    }

    /**
     * Получение периода начисления.
     */
    public Optional<BillingPeriod> getBillingPeriod(int accountId, String period) throws SQLException {
        getBillingPeriodStmt.setInt(1, accountId);
        getBillingPeriodStmt.setString(2, period);
        try (ResultSet rs = getBillingPeriodStmt.executeQuery()) {
            if (rs.next()) {
                return Optional.of(new BillingPeriod(
                    rs.getInt("id"),
                    rs.getBigDecimal("total_amount")
                ));
            }
        }
        return Optional.empty();
    }

    /**
     * Создание или обновление периода начисления.
     * Возвращает ID периода.
     */
    public int createOrUpdateBillingPeriod(int accountId, String period, double totalAmount) throws SQLException {
        insertBillingPeriodStmt.setInt(1, accountId);
        insertBillingPeriodStmt.setString(2, period);
        insertBillingPeriodStmt.setDouble(3, totalAmount);
        insertBillingPeriodStmt.executeUpdate();

        try (ResultSet rs = insertBillingPeriodStmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        // Если не вставился, получаем существующий
        Optional<BillingPeriod> existing = getBillingPeriod(accountId, period);
        if (existing.isPresent()) {
            return existing.get().id();
        }

        throw new SQLException("Не удалось создать период начисления");
    }

    /**
     * Добавление начислений по приборам учета.
     * Использует UPSERT (INSERT ... ON CONFLICT DO UPDATE) для избежания дублирования.
     */
    public void addMeterCharges(int accountId, String period, List<MeterCharge> charges) throws SQLException {
        for (MeterCharge charge : charges) {
            insertOrUpdateMeterChargeStmt.setInt(1, accountId);
            insertOrUpdateMeterChargeStmt.setString(2, period);
            insertOrUpdateMeterChargeStmt.setString(3, charge.meterName());
            if (charge.reading() != null) {
                insertOrUpdateMeterChargeStmt.setDouble(4, charge.reading());
            } else {
                insertOrUpdateMeterChargeStmt.setNull(4, java.sql.Types.DOUBLE);
            }
            if (charge.amount() != null) {
                insertOrUpdateMeterChargeStmt.setDouble(5, charge.amount());
            } else {
                insertOrUpdateMeterChargeStmt.setNull(5, java.sql.Types.DOUBLE);
            }
            insertOrUpdateMeterChargeStmt.addBatch();
        }
        insertOrUpdateMeterChargeStmt.executeBatch();
    }

    /**
     * Добавление одного начисления по прибору учета.
     */
    public void addMeterCharge(int accountId, String period, String meterName, Double reading, Double amount) throws SQLException {
        insertOrUpdateMeterChargeStmt.setInt(1, accountId);
        insertOrUpdateMeterChargeStmt.setString(2, period);
        insertOrUpdateMeterChargeStmt.setString(3, meterName);
        if (reading != null) {
            insertOrUpdateMeterChargeStmt.setDouble(4, reading);
        } else {
            insertOrUpdateMeterChargeStmt.setNull(4, java.sql.Types.DOUBLE);
        }
        if (amount != null) {
            insertOrUpdateMeterChargeStmt.setDouble(5, amount);
        } else {
            insertOrUpdateMeterChargeStmt.setNull(5, java.sql.Types.DOUBLE);
        }
        insertOrUpdateMeterChargeStmt.executeUpdate();
    }

    /**
     * Закрытие prepared statements.
     */
    public void close() {
        try {
            if (getBillingPeriodStmt != null) getBillingPeriodStmt.close();
            if (insertBillingPeriodStmt != null) insertBillingPeriodStmt.close();
            if (getMeterChargeStmt != null) getMeterChargeStmt.close();
            if (insertOrUpdateMeterChargeStmt != null) insertOrUpdateMeterChargeStmt.close();
        } catch (SQLException e) {
            // Игнорируем ошибки при закрытии
        }
    }

    /**
     * Модель периода начисления.
     */
    public record BillingPeriod(int id, java.math.BigDecimal totalAmount) {}

    /**
     * Модель начисления по прибору учета.
     */
    public record MeterCharge(String meterName, Double reading, Double amount) {}
}
