package org.example.repository.impl;

import org.example.exception.DatabaseException;
import org.example.parser.model.Apartment;
import org.example.repository.ApartmentRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC реализация репозитория квартир
 */
public class JdbcApartmentRepository implements ApartmentRepository {
    private final DataSource dataSource;
    
    public JdbcApartmentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Optional<Apartment> findById(Long id) {
        String sql = "SELECT id, house_id, number FROM apartments WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding apartment by id: " + id, e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<Apartment> findAll() {
        String sql = "SELECT id, house_id, number FROM apartments ORDER BY number";
        List<Apartment> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all apartments", e);
        }
        return result;
    }
    
    @Override
    public List<Apartment> findByHouseId(Long houseId) {
        String sql = "SELECT id, house_id, number FROM apartments WHERE house_id = ? ORDER BY number";
        List<Apartment> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, houseId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding apartments by house id: " + houseId, e);
        }
        return result;
    }
    
    @Override
    public List<Apartment> findByHouseIdAndNumberContaining(Long houseId, String number) {
        String sql = "SELECT id, house_id, number FROM apartments WHERE house_id = ? AND number ILIKE ? ORDER BY number LIMIT 100";
        List<Apartment> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, houseId);
            stmt.setString(2, "%" + number + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding apartments by house and number", e);
        }
        return result;
    }
    
    @Override
    public Optional<Apartment> findByHouseIdAndNumber(Long houseId, String number) {
        String sql = "SELECT id, house_id, number FROM apartments WHERE house_id = ? AND number = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, houseId);
            stmt.setString(2, number);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding apartment by house and number", e);
        }
        return Optional.empty();
    }
    
    @Override
    public Apartment save(Apartment entity) {
        String sql = "INSERT INTO apartments (house_id, number) VALUES (?, ?) " +
                     "ON CONFLICT (house_id, number) DO NOTHING " +
                     "RETURNING id, house_id, number";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, entity.getHouseId());
            stmt.setString(2, entity.getNumber());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving apartment", e);
        }
        // Если уже существует, найдем и вернем
        return findByHouseIdAndNumber(entity.getHouseId(), entity.getNumber()).orElse(null);
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM apartments WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting apartment: " + id, e);
        }
    }
    
    private Apartment mapRow(ResultSet rs) throws SQLException {
        Apartment apartment = new Apartment();
        apartment.setId(rs.getLong("id"));
        apartment.setHouseId(rs.getLong("house_id"));
        apartment.setNumber(rs.getString("number"));
        return apartment;
    }
}
