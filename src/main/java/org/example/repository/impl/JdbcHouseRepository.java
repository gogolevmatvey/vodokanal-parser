package org.example.repository.impl;

import org.example.exception.DatabaseException;
import org.example.parser.model.House;
import org.example.repository.HouseRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC реализация репозитория домов
 */
public class JdbcHouseRepository implements HouseRepository {
    private final DataSource dataSource;
    
    public JdbcHouseRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Optional<House> findById(Long id) {
        String sql = "SELECT id, street_id, number, building FROM houses WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding house by id: " + id, e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<House> findAll() {
        String sql = "SELECT id, street_id, number, building FROM houses ORDER BY number";
        List<House> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all houses", e);
        }
        return result;
    }
    
    @Override
    public List<House> findByStreetId(Long streetId) {
        String sql = "SELECT id, street_id, number, building FROM houses WHERE street_id = ? ORDER BY number";
        List<House> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, streetId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding houses by street id: " + streetId, e);
        }
        return result;
    }
    
    @Override
    public List<House> findByStreetIdAndNumberContaining(Long streetId, String number) {
        String sql = "SELECT id, street_id, number, building FROM houses WHERE street_id = ? AND number ILIKE ? ORDER BY number LIMIT 100";
        List<House> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, streetId);
            stmt.setString(2, "%" + number + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding houses by street and number", e);
        }
        return result;
    }
    
    @Override
    public Optional<House> findByStreetIdAndNumber(Long streetId, String number) {
        String sql = "SELECT id, street_id, number, building FROM houses WHERE street_id = ? AND number = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, streetId);
            stmt.setString(2, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding house by street and number", e);
        }
        return Optional.empty();
    }
    
    @Override
    public House save(House entity) {
        String sql = "INSERT INTO houses (street_id, number, building) VALUES (?, ?, ?) " +
                     "ON CONFLICT (street_id, number) DO UPDATE SET building = EXCLUDED.building " +
                     "RETURNING id, street_id, number, building";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, entity.getStreetId());
            stmt.setString(2, entity.getNumber());
            stmt.setString(3, entity.getBuilding());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving house", e);
        }
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM houses WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting house: " + id, e);
        }
    }
    
    private House mapRow(ResultSet rs) throws SQLException {
        House house = new House();
        house.setId(rs.getLong("id"));
        house.setStreetId(rs.getLong("street_id"));
        house.setNumber(rs.getString("number"));
        house.setBuilding(rs.getString("building"));
        return house;
    }
}
