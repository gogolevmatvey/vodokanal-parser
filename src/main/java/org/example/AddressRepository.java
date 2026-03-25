package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Репозиторий для работы с адресами (localities, streets, houses, apartments).
 * Реализует паттерн "upsert" (insert or get existing).
 */
public class AddressRepository {

    private final DatabaseManager dbManager;

    // Prepared statements для кэширования
    private PreparedStatement getLocalityStmt;
    private PreparedStatement insertLocalityStmt;
    private PreparedStatement getStreetStmt;
    private PreparedStatement insertStreetStmt;
    private PreparedStatement getHouseStmt;
    private PreparedStatement insertHouseStmt;
    private PreparedStatement getApartmentStmt;
    private PreparedStatement insertApartmentStmt;

    public AddressRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Инициализация prepared statements.
     */
    public void init() throws SQLException {
        Connection conn = dbManager.getConnection();
        
        getLocalityStmt = conn.prepareStatement("SELECT id FROM localities WHERE name = ?");
        insertLocalityStmt = conn.prepareStatement(
            "INSERT INTO localities (name) VALUES (?) ON CONFLICT (name) DO NOTHING RETURNING id",
            PreparedStatement.RETURN_GENERATED_KEYS
        );

        getStreetStmt = conn.prepareStatement(
            "SELECT id FROM streets WHERE locality_id = ? AND name = ?"
        );
        insertStreetStmt = conn.prepareStatement(
            "INSERT INTO streets (locality_id, name) VALUES (?, ?) ON CONFLICT (locality_id, name) DO NOTHING RETURNING id",
            PreparedStatement.RETURN_GENERATED_KEYS
        );

        getHouseStmt = conn.prepareStatement(
            "SELECT id FROM houses WHERE street_id = ? AND number = ?"
        );
        insertHouseStmt = conn.prepareStatement(
            "INSERT INTO houses (street_id, number) VALUES (?, ?) ON CONFLICT (street_id, number) DO NOTHING RETURNING id",
            PreparedStatement.RETURN_GENERATED_KEYS
        );

        getApartmentStmt = conn.prepareStatement(
            "SELECT id FROM apartments WHERE house_id = ? AND number = ?"
        );
        insertApartmentStmt = conn.prepareStatement(
            "INSERT INTO apartments (house_id, number) VALUES (?, ?) ON CONFLICT (house_id, number) DO NOTHING RETURNING id",
            PreparedStatement.RETURN_GENERATED_KEYS
        );
    }

    /**
     * Получение или создание населенного пункта.
     */
    private int getOrCreateLocality(Connection conn, String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new SQLException("Название населенного пункта не может быть пустым");
        }
        name = name.trim();

        // Пробуем найти существующий
        getLocalityStmt.setString(1, name);
        try (ResultSet rs = getLocalityStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        // Создаем новый
        insertLocalityStmt.setString(1, name);
        insertLocalityStmt.executeUpdate();
        try (ResultSet rs = insertLocalityStmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        // Если не получилось вставить (конкуренция), получаем существующий
        getLocalityStmt.setString(1, name);
        try (ResultSet rs = getLocalityStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        throw new SQLException("Не удалось создать или получить населенный пункт: " + name);
    }

    /**
     * Получение или создание улицы.
     */
    private int getOrCreateStreet(Connection conn, int localityId, String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new SQLException("Название улицы не может быть пустым");
        }
        name = name.trim();

        getStreetStmt.setInt(1, localityId);
        getStreetStmt.setString(2, name);
        try (ResultSet rs = getStreetStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        insertStreetStmt.setInt(1, localityId);
        insertStreetStmt.setString(2, name);
        insertStreetStmt.executeUpdate();
        try (ResultSet rs = insertStreetStmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        getStreetStmt.setInt(1, localityId);
        getStreetStmt.setString(2, name);
        try (ResultSet rs = getStreetStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        throw new SQLException("Не удалось создать или получить улицу: " + name);
    }

    /**
     * Получение или создание дома.
     */
    private int getOrCreateHouse(Connection conn, int streetId, String number) throws SQLException {
        if (number == null || number.trim().isEmpty()) {
            throw new SQLException("Номер дома не может быть пустым");
        }
        number = number.trim();

        getHouseStmt.setInt(1, streetId);
        getHouseStmt.setString(2, number);
        try (ResultSet rs = getHouseStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        insertHouseStmt.setInt(1, streetId);
        insertHouseStmt.setString(2, number);
        insertHouseStmt.executeUpdate();
        try (ResultSet rs = insertHouseStmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        getHouseStmt.setInt(1, streetId);
        getHouseStmt.setString(2, number);
        try (ResultSet rs = getHouseStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        throw new SQLException("Не удалось создать или получить дом: " + number);
    }

    /**
     * Получение или создание квартиры.
     */
    private int getOrCreateApartment(Connection conn, int houseId, String number) throws SQLException {
        if (number == null || number.trim().isEmpty()) {
            throw new SQLException("Номер квартиры не может быть пустым");
        }
        number = number.trim();

        getApartmentStmt.setInt(1, houseId);
        getApartmentStmt.setString(2, number);
        try (ResultSet rs = getApartmentStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        insertApartmentStmt.setInt(1, houseId);
        insertApartmentStmt.setString(2, number);
        insertApartmentStmt.executeUpdate();
        try (ResultSet rs = insertApartmentStmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        getApartmentStmt.setInt(1, houseId);
        getApartmentStmt.setString(2, number);
        try (ResultSet rs = getApartmentStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        throw new SQLException("Не удалось создать или получить квартиру: " + number);
    }

    /**
     * Получение или создание полного адреса.
     * Возвращает ID квартиры.
     */
    public int getOrCreateAddress(String locality, String street, String house, String apartment) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            int localityId = getOrCreateLocality(conn, locality);
            int streetId = getOrCreateStreet(conn, localityId, street);
            int houseId = getOrCreateHouse(conn, streetId, house);
            
            // Если квартира не указана, используем "0" или "не указано"
            String aptNumber = (apartment != null && !apartment.trim().isEmpty()) 
                ? apartment.trim() 
                : "0";
            
            return getOrCreateApartment(conn, houseId, aptNumber);
        }
    }

    /**
     * Закрытие prepared statements.
     */
    public void close() {
        try {
            if (getLocalityStmt != null) getLocalityStmt.close();
            if (insertLocalityStmt != null) insertLocalityStmt.close();
            if (getStreetStmt != null) getStreetStmt.close();
            if (insertStreetStmt != null) insertStreetStmt.close();
            if (getHouseStmt != null) getHouseStmt.close();
            if (insertHouseStmt != null) insertHouseStmt.close();
            if (getApartmentStmt != null) getApartmentStmt.close();
            if (insertApartmentStmt != null) insertApartmentStmt.close();
        } catch (SQLException e) {
            // Игнорируем ошибки при закрытии
        }
    }
}
