package org.example.config;

import org.example.repository.*;
import org.example.repository.impl.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Конфигурация для репозиториев и подключений к БД
 */
@Configuration
@ComponentScan(basePackages = {
    "org.example.service",
    "org.example.etl"
})
public class DatabaseConfig {

    @Bean
    public ConfigManager configManager() {
        return ConfigManager.getInstance();
    }

    @Bean
    public DatabaseManager databaseManager() {
        return new DatabaseManager();
    }

    @Bean
    public DataSource dataSource(DatabaseManager databaseManager) {
        return databaseManager.getDataSource();
    }

    @Bean
    public LocalityRepository localityRepository(DataSource dataSource) {
        return new JdbcLocalityRepository(dataSource);
    }

    @Bean
    public StreetRepository streetRepository(DataSource dataSource) {
        return new JdbcStreetRepository(dataSource);
    }

    @Bean
    public HouseRepository houseRepository(DataSource dataSource) {
        return new JdbcHouseRepository(dataSource);
    }

    @Bean
    public ApartmentRepository apartmentRepository(DataSource dataSource) {
        return new JdbcApartmentRepository(dataSource);
    }

    @Bean
    public AccountRepository accountRepository(DataSource dataSource) {
        return new JdbcAccountRepository(dataSource);
    }

    @Bean
    public BillingPeriodRepository billingPeriodRepository(DataSource dataSource) {
        return new JdbcBillingPeriodRepository(dataSource);
    }

    @Bean
    public MeterChargeRepository meterChargeRepository(DataSource dataSource) {
        return new JdbcMeterChargeRepository(dataSource);
    }
}
