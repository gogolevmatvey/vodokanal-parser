package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Менеджер конфигурации приложения.
 * Загружает настройки из config.properties
 */
public class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private final Properties properties;

    private static ConfigManager instance;

    private ConfigManager() {
        properties = new Properties();
        loadProperties();
    }

    /**
     * Получить единственный экземпляр ConfigManager
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Загрузка свойств из файла
     */
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Файл конфигурации не найден: " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки конфигурации: " + e.getMessage(), e);
        }
    }

    /**
     * Получить строковое свойство
     */
    public String getString(String key, String defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? value.trim() : defaultValue;
    }

    /**
     * Получить строковое свойство (обязательное)
     */
    public String getString(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Обязательное свойство не найдено: " + key);
        }
        return value.trim();
    }

    /**
     * Получить целочисленное свойство
     */
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Неверный формат числа для свойства " + key + ": " + value, e);
        }
    }

    /**
     * Получить свойство в виде списка (разделитель - запятая)
     */
    public List<String> getList(String key, List<String> defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Arrays.asList(value.split("\\s*,\\s*"));
    }

    /**
     * Получить булево свойство
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    // ====================================
    // Методы для доступа к настройкам БД
    // ====================================

    public String getDbHost() {
        return getString("db.host", "localhost");
    }

    public int getDbPort() {
        return getInt("db.port", 5432);
    }

    public String getDbName() {
        return getString("db.name", "vodokanal-db");
    }

    public String getDbUser() {
        return getString("db.user", "postgres");
    }

    public String getDbPassword() {
        return getString("db.password", "postgres");
    }

    // ====================================
    // Методы для доступа к путям файлов
    // ====================================

    public String getFileInput() {
        return getString("file.input", "Testovye_dannye (1).txt");
    }

    public String getFileOutputValid() {
        return getString("file.output.valid", "valid_records.txt");
    }

    public String getFileOutputInvalid() {
        return getString("file.output.invalid", "invalid_records.txt");
    }

    public String getFileOutputCorrected() {
        return getString("file.output.corrected", "corrected_records.txt");
    }

    public String getFileOutputUncorrected() {
        return getString("file.output.uncorrected", "uncorrected_records.txt");
    }

    public String getFileOutputErrorReport() {
        return getString("file.output.error-report", "error_report.txt");
    }

    // ====================================
    // Методы для доступа к CORS настройкам
    // ====================================

    public List<String> getCorsAllowedOrigins() {
        return getList("cors.allowed-origins", Arrays.asList("http://localhost:3000", "http://localhost:5173"));
    }

    public List<String> getCorsAllowedMethods() {
        return getList("cors.allowed-methods", Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    }

    public String getCorsAllowedHeaders() {
        return getString("cors.allowed-headers", "*");
    }

    public boolean isCorsAllowCredentials() {
        return getBoolean("cors.allow-credentials", true);
    }
}
