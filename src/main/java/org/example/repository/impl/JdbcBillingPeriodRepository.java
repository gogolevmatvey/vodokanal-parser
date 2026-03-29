package org.example.repository.impl;

import org.example.exception.DatabaseException;
import org.example.model.domain.BillingPeriod;
import org.example.repository.BillingPeriodRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC реализация репозитория периодов начислений
 */
public class JdbcBillingPeriodRepository implements BillingPeriodRepository {
    private final DataSource dataSource;
    
    public JdbcBillingPeriodRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Optional<BillingPeriod> findById(Long id) {
        String sql = "SELECT id, account_id, period, total_amount FROM billing_periods WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding billing period by id: " + id, e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<BillingPeriod> findAll() {
        String sql = "SELECT id, account_id, period, total_amount FROM billing_periods ORDER BY period";
        List<BillingPeriod> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all billing periods", e);
        }
        return result;
    }
    
    @Override
    public Optional<BillingPeriod> findByAccountIdAndPeriod(Long accountId, String period) {
        String sql = "SELECT id, account_id, period, total_amount FROM billing_periods WHERE account_id = ? AND period = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, accountId);
            stmt.setString(2, period);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding billing period by account and period", e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<BillingPeriod> findByAccountId(Long accountId) {
        String sql = "SELECT id, account_id, period, total_amount FROM billing_periods WHERE account_id = ? ORDER BY period DESC";
        List<BillingPeriod> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, accountId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding billing periods by account id: " + accountId, e);
        }
        return result;
    }
    
    @Override
    public BillingPeriod save(BillingPeriod entity) {
        String sql = "INSERT INTO billing_periods (account_id, period, total_amount) VALUES (?, ?, ?) " +
                     "ON CONFLICT (account_id, period) DO UPDATE SET total_amount = EXCLUDED.total_amount " +
                     "RETURNING id, account_id, period, total_amount";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, entity.getAccountId());
            stmt.setString(2, entity.getPeriod());
            stmt.setBigDecimal(3, entity.getTotalAmount());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving billing period", e);
        }
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM billing_periods WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting billing period: " + id, e);
        }
    }
    
    private BillingPeriod mapRow(ResultSet rs) throws SQLException {
        BillingPeriod period = new BillingPeriod();
        period.setId(rs.getLong("id"));
        period.setAccountId(rs.getLong("account_id"));
        period.setPeriod(rs.getString("period"));
        period.setTotalAmount(rs.getBigDecimal("total_amount"));
        return period;
    }
}
