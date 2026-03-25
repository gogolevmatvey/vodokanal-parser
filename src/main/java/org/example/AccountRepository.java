package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Репозиторий для работы с лицевыми счетами (accounts).
 */
public class AccountRepository {

    private final DatabaseManager dbManager;

    private PreparedStatement getAccountStmt;
    private PreparedStatement insertAccountStmt;
    private PreparedStatement updateAccountStmt;

    public AccountRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Инициализация prepared statements.
     */
    public void init() throws SQLException {
        Connection conn = dbManager.getConnection();

        getAccountStmt = conn.prepareStatement(
            "SELECT id, payer_name, apartment_id FROM accounts WHERE account_number = ?"
        );
        insertAccountStmt = conn.prepareStatement(
            "INSERT INTO accounts (account_number, payer_name, apartment_id) VALUES (?, ?, ?) " +
            "ON CONFLICT (account_number) DO NOTHING RETURNING id",
            PreparedStatement.RETURN_GENERATED_KEYS
        );
        updateAccountStmt = conn.prepareStatement(
            "UPDATE accounts SET payer_name = ?, apartment_id = ? WHERE account_number = ?"
        );
    }

    /**
     * Получение лицевого счета по номеру.
     */
    public Optional<Account> getAccount(String accountNumber) throws SQLException {
        getAccountStmt.setString(1, accountNumber);
        try (ResultSet rs = getAccountStmt.executeQuery()) {
            if (rs.next()) {
                return Optional.of(new Account(
                    rs.getInt("id"),
                    rs.getString("payer_name"),
                    rs.getObject("apartment_id", Integer.class)
                ));
            }
        }
        return Optional.empty();
    }

    /**
     * Создание или обновление лицевого счета.
     * Возвращает ID счета.
     */
    public int createOrUpdateAccount(String accountNumber, String payerName, Integer apartmentId) throws SQLException {
        // Пробуем вставить новый
        insertAccountStmt.setString(1, accountNumber);
        insertAccountStmt.setString(2, payerName);
        if (apartmentId != null) {
            insertAccountStmt.setInt(3, apartmentId);
        } else {
            insertAccountStmt.setNull(3, java.sql.Types.INTEGER);
        }
        insertAccountStmt.executeUpdate();

        // Проверяем, удалось ли вставить
        try (ResultSet rs = insertAccountStmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        // Если не вставился (уже существует), обновляем и получаем ID
        updateAccountStmt.setString(1, payerName);
        if (apartmentId != null) {
            updateAccountStmt.setInt(2, apartmentId);
        } else {
            updateAccountStmt.setNull(2, java.sql.Types.INTEGER);
        }
        updateAccountStmt.setString(3, accountNumber);
        updateAccountStmt.executeUpdate();

        // Получаем существующий счет
        Optional<Account> existing = getAccount(accountNumber);
        if (existing.isPresent()) {
            return existing.get().id();
        }

        throw new SQLException("Не удалось создать или получить лицевой счет: " + accountNumber);
    }

    /**
     * Закрытие prepared statements.
     */
    public void close() {
        try {
            if (getAccountStmt != null) getAccountStmt.close();
            if (insertAccountStmt != null) insertAccountStmt.close();
            if (updateAccountStmt != null) updateAccountStmt.close();
        } catch (SQLException e) {
            // Игнорируем ошибки при закрытии
        }
    }

    /**
     * Модель лицевого счета.
     */
    public record Account(int id, String payerName, Integer apartmentId) {}
}
