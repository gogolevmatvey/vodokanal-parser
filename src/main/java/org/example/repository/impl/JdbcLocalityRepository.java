package org.example.repository.impl;

import org.example.exception.DatabaseException;
import org.example.model.domain.Locality;
import org.example.repository.LocalityRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC реализация репозитория населенных пунктов
 */
public class JdbcLocalityRepository implements LocalityRepository {
    private final DataSource dataSource;
    
    public JdbcLocalityRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public Optional<Locality> findById(Long id) {
        String sql = "SELECT id, name FROM localities WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding locality by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Locality> findAll() {
        String sql = "SELECT id, name FROM localities ORDER BY name";
        List<Locality> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding all localities", e);
        }
        return result;
    }

    @Override
    public List<Locality> findByNameContaining(String name) {
        String sql = "SELECT id, name FROM localities WHERE name ILIKE ? ORDER BY name LIMIT 100";
        List<Locality> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + name + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding localities by name: " + name, e);
        }
        return result;
    }

    @Override
    public Optional<Locality> findByName(String name) {
        String sql = "SELECT id, name FROM localities WHERE name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding locality by name: " + name, e);
        }
        return Optional.empty();
    }

    @Override
    public Locality save(Locality entity) {
        String sql = "INSERT INTO localities (name) VALUES (?) " +
                     "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name " +
                     "RETURNING id, name";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, entity.getName());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error saving locality", e);
        }
        return null;
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM localities WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting locality: " + id, e);
        }
    }
    
    private Locality mapRow(ResultSet rs) throws SQLException {
        Locality locality = new Locality();
        locality.setId(rs.getLong("id"));
        locality.setName(rs.getString("name"));
        return locality;
    }
}
