package org.example.repository.impl;

import org.example.exception.DatabaseException;
import org.example.model.domain.Account;
import org.example.repository.AccountRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC реализация репозитория лицевых счетов
 */
public class JdbcAccountRepository implements AccountRepository {
    private final DataSource dataSource;
    
    public JdbcAccountRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Optional<Account> findById(Long id) {
        String sql = "SELECT id, apartment_id, account_number, payer_name FROM accounts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding account by id: " + id, e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<Account> findAll() {
        String sql = "SELECT id, apartment_id, account_number, payer_name FROM accounts ORDER BY account_number";
        List<Account> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all accounts", e);
        }
        return result;
    }
    
    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT id, apartment_id, account_number, payer_name FROM accounts WHERE account_number = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding account by number: " + accountNumber, e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<Account> findByApartmentId(Long apartmentId) {
        String sql = "SELECT id, apartment_id, account_number, payer_name FROM accounts WHERE apartment_id = ? ORDER BY account_number";
        List<Account> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, apartmentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding accounts by apartment id: " + apartmentId, e);
        }
        return result;
    }
    
    @Override
    public Optional<Account> findByApartmentIdAndAccountNumber(Long apartmentId, String accountNumber) {
        String sql = "SELECT id, apartment_id, account_number, payer_name FROM accounts WHERE apartment_id = ? AND account_number = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, apartmentId);
            stmt.setString(2, accountNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding account by apartment and number", e);
        }
        return Optional.empty();
    }
    
    @Override
    public Account save(Account entity) {
        String sql = "INSERT INTO accounts (apartment_id, account_number, payer_name) VALUES (?, ?, ?) " +
                     "ON CONFLICT (account_number) DO UPDATE SET apartment_id = EXCLUDED.apartment_id, payer_name = EXCLUDED.payer_name " +
                     "RETURNING id, apartment_id, account_number, payer_name";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, entity.getApartmentId());
            stmt.setString(2, entity.getAccountNumber());
            stmt.setString(3, entity.getPayerName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving account", e);
        }
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM accounts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting account: " + id, e);
        }
    }
    
    private Account mapRow(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setApartmentId(rs.getLong("apartment_id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setPayerName(rs.getString("payer_name"));
        return account;
    }
}
