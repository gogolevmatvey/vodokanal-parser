package org.example.repository.impl;

import org.example.exception.DatabaseException;
import org.example.parser.model.Street;
import org.example.repository.StreetRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC реализация репозитория улиц
 */
public class JdbcStreetRepository implements StreetRepository {
    private final DataSource dataSource;
    
    public JdbcStreetRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Optional<Street> findById(Long id) {
        String sql = "SELECT id, locality_id, name, type FROM streets WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding street by id: " + id, e);
        }
        return Optional.empty();
    }
    
    @Override
    public List<Street> findAll() {
        String sql = "SELECT id, locality_id, name, type FROM streets ORDER BY name";
        List<Street> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all streets", e);
        }
        return result;
    }
    
    @Override
    public List<Street> findByLocalityId(Long localityId) {
        String sql = "SELECT id, locality_id, name, type FROM streets WHERE locality_id = ? ORDER BY name";
        List<Street> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, localityId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding streets by locality id: " + localityId, e);
        }
        return result;
    }
    
    @Override
    public List<Street> findByLocalityIdAndNameContaining(Long localityId, String name) {
        String sql = "SELECT id, locality_id, name, type FROM streets WHERE locality_id = ? AND name ILIKE ? ORDER BY name LIMIT 100";
        List<Street> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, localityId);
            stmt.setString(2, "%" + name + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding streets by locality and name", e);
        }
        return result;
    }
    
    @Override
    public Optional<Street> findByLocalityIdAndName(Long localityId, String name) {
        String sql = "SELECT id, locality_id, name, type FROM streets WHERE locality_id = ? AND name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, localityId);
            stmt.setString(2, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding street by locality and name", e);
        }
        return Optional.empty();
    }
    
    @Override
    public Street save(Street entity) {
        String sql = "INSERT INTO streets (locality_id, name, type) VALUES (?, ?, ?) " +
                     "ON CONFLICT (locality_id, name) DO UPDATE SET type = EXCLUDED.type " +
                     "RETURNING id, locality_id, name, type";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, entity.getLocalityId());
            stmt.setString(2, entity.getName());
            stmt.setString(3, entity.getType());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving street", e);
        }
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM streets WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting street: " + id, e);
        }
    }
    
    private Street mapRow(ResultSet rs) throws SQLException {
        Street street = new Street();
        street.setId(rs.getLong("id"));
        street.setLocalityId(rs.getLong("locality_id"));
        street.setName(rs.getString("name"));
        street.setType(rs.getString("type"));
        return street;
    }
}
