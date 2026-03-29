package org.example.repository.impl;

import org.example.exception.DatabaseException;
import org.example.model.domain.MeterCharge;
import org.example.repository.MeterChargeRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC реализация репозитория начислений по приборам учета
 */
public class JdbcMeterChargeRepository implements MeterChargeRepository {
    private final DataSource dataSource;
    
    public JdbcMeterChargeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Optional<MeterCharge> findById(Long id) {
        String sql = "SELECT id, billing_period_id, meter_name, reading, amount FROM meter_charges WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding meter charge by id: " + id, e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<MeterCharge> findAll() {
        String sql = "SELECT id, billing_period_id, meter_name, reading, amount FROM meter_charges ORDER BY meter_name";
        List<MeterCharge> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all meter charges", e);
        }
        return result;
    }
    
    @Override
    public List<MeterCharge> findByBillingPeriodId(Long billingPeriodId) {
        String sql = "SELECT id, billing_period_id, meter_name, reading, amount FROM meter_charges WHERE billing_period_id = ? ORDER BY meter_name";
        List<MeterCharge> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, billingPeriodId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding meter charges by billing period id: " + billingPeriodId, e);
        }
        return result;
    }
    
    @Override
    public Optional<MeterCharge> findByBillingPeriodIdAndMeterName(Long billingPeriodId, String meterName) {
        String sql = "SELECT id, billing_period_id, meter_name, reading, amount FROM meter_charges WHERE billing_period_id = ? AND meter_name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, billingPeriodId);
            stmt.setString(2, meterName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding meter charge by billing period and meter name", e);
        }
        return Optional.empty();
    }
    
    @Override
    public MeterCharge save(MeterCharge entity) {
        String sql = "INSERT INTO meter_charges (billing_period_id, meter_name, reading, amount) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (billing_period_id, meter_name) DO UPDATE SET reading = EXCLUDED.reading, amount = EXCLUDED.amount " +
                     "RETURNING id, billing_period_id, meter_name, reading, amount";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, entity.getBillingPeriodId());
            stmt.setString(2, entity.getMeterName());
            stmt.setBigDecimal(3, entity.getReading());
            stmt.setBigDecimal(4, entity.getAmount());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving meter charge", e);
        }
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM meter_charges WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting meter charge: " + id, e);
        }
    }
    
    private MeterCharge mapRow(ResultSet rs) throws SQLException {
        MeterCharge charge = new MeterCharge();
        charge.setId(rs.getLong("id"));
        charge.setBillingPeriodId(rs.getLong("billing_period_id"));
        charge.setMeterName(rs.getString("meter_name"));
        charge.setReading(rs.getBigDecimal("reading"));
        charge.setAmount(rs.getBigDecimal("amount"));
        return charge;
    }
}
