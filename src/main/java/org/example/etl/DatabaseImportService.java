package org.example.etl;

import org.example.model.common.ParseResult;
import org.example.model.common.ParsedRecord;
import org.example.model.common.Address;
import org.example.model.domain.*;
import org.example.repository.*;
import org.example.service.parsing.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Сервис для импорта данных в базу данных
 */
@Service
public class DatabaseImportService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseImportService.class);

    private final LocalityRepository localityRepo;
    private final StreetRepository streetRepo;
    private final HouseRepository houseRepo;
    private final ApartmentRepository apartmentRepo;
    private final AccountRepository accountRepo;
    private final BillingPeriodRepository billingPeriodRepo;
    private final MeterChargeRepository meterChargeRepo;
    private final RecordParser parser;

    public DatabaseImportService(LocalityRepository localityRepo,
                                 StreetRepository streetRepo,
                                 HouseRepository houseRepo,
                                 ApartmentRepository apartmentRepo,
                                 AccountRepository accountRepo,
                                 BillingPeriodRepository billingPeriodRepo,
                                 MeterChargeRepository meterChargeRepo,
                                 RecordParser parser) {
        this.localityRepo = localityRepo;
        this.streetRepo = streetRepo;
        this.houseRepo = houseRepo;
        this.apartmentRepo = apartmentRepo;
        this.accountRepo = accountRepo;
        this.billingPeriodRepo = billingPeriodRepo;
        this.meterChargeRepo = meterChargeRepo;
        this.parser = parser;
    }

    /**
     * Результат пакетной вставки в БД
     */
    public static class BatchResult {
        private final int savedCount;
        private final int errorCount;
        private final Map<String, Integer> errorCounts;
        private final Map<String, List<String>> errorSamples;

        public BatchResult(int savedCount, int errorCount,
                          Map<String, Integer> errorCounts,
                          Map<String, List<String>> errorSamples) {
            this.savedCount = savedCount;
            this.errorCount = errorCount;
            this.errorCounts = errorCounts;
            this.errorSamples = errorSamples;
        }

        public int getSavedCount() { return savedCount; }
        public int getErrorCount() { return errorCount; }
        public Map<String, Integer> getErrorCounts() { return errorCounts; }
        public Map<String, List<String>> getErrorSamples() { return errorSamples; }
    }

    /**
     * Сохранить пакет записей в базу данных
     */
    public BatchResult saveBatch(List<String> records, int maxErrorSamples) {
        int savedCount = 0;
        int errorCount = 0;
        Map<String, Integer> errorCounts = new java.util.TreeMap<>();
        Map<String, List<String>> errorSamples = new java.util.HashMap<>();

        for (String line : records) {
            try {
                saveRecord(line);
                savedCount++;
            } catch (Exception e) {
                errorCount++;
                String errorMsg = e.getMessage();
                String errorType = extractDbErrorType(errorMsg);

                errorCounts.put(errorType, errorCounts.getOrDefault(errorType, 0) + 1);

                // Сохраняем примеры (до maxErrorSamples на каждый тип)
                errorSamples.computeIfAbsent(errorType, k -> new java.util.ArrayList<>());
                if (errorSamples.get(errorType).size() < maxErrorSamples) {
                    errorSamples.get(errorType).add(line.substring(0, Math.min(150, line.length())));
                }
            }
        }

        return new BatchResult(savedCount, errorCount, errorCounts, errorSamples);
    }

    /**
     * Сохранить одну запись в базу данных
     */
    private void saveRecord(String line) throws SQLException {
        var result = parser.parse(line);
        if (!result.isValid()) {
            throw new SQLException("Невозможно сохранить невалидную запись: " + result.getErrorMessage());
        }

        ParsedRecord data = result.getRecordData();

        // 1. Сохраняем адрес (locality -> street -> house -> apartment)
        Address address = data.getAddress();
        String[] addressParts = address.getFullAddress().split(",");
        String localityName = addressParts[0].trim();
        String streetName = addressParts.length > 1 ? addressParts[1].trim() : "Улица не указана";
        String houseNumber = addressParts.length > 2 ? addressParts[2].trim() : "Дом не указан";
        String apartmentNumber = addressParts.length > 3 ? addressParts[3].trim() : "0";

        try {
            // Сохраняем или находим населенный пункт
            log.debug("Поиск населенного пункта: {}", localityName);
            Locality locality = localityRepo.findByName(localityName)
                .orElseGet(() -> {
                    log.debug("Населенный пункт не найден, создаем: {}", localityName);
                    return localityRepo.save(new Locality(null, localityName, null));
                });
            log.trace("Населенный пункт: id={}, name={}", locality.getId(), locality.getName());

            // Сохраняем или находим улицу
            Street street = streetRepo.findByLocalityIdAndName(locality.getId(), streetName)
                .orElseGet(() -> {
                    log.debug("Улица не найдена, создаем: {} (localityId={})", streetName, locality.getId());
                    return streetRepo.save(new Street(null, locality.getId(), streetName, null));
                });
            log.trace("Улица: id={}, name={}", street.getId(), street.getName());

            // Сохраняем или находим дом
            House house = houseRepo.findByStreetIdAndNumber(street.getId(), houseNumber)
                .orElseGet(() -> {
                    log.debug("Дом не найден, создаем: {} (streetId={})", houseNumber, street.getId());
                    return houseRepo.save(new House(null, street.getId(), houseNumber, null));
                });
            log.trace("Дом: id={}, number={}", house.getId(), house.getNumber());

            // Сохраняем или находим квартиру
            Apartment apartment = apartmentRepo.findByHouseIdAndNumber(house.getId(), apartmentNumber)
                .orElseGet(() -> {
                    log.debug("Квартира не найдена, создаем: {} (houseId={})", apartmentNumber, house.getId());
                    return apartmentRepo.save(new Apartment(null, house.getId(), apartmentNumber));
                });
            log.trace("Квартира: id={}, number={}", apartment.getId(), apartment.getNumber());

            // 2. Сохраняем лицевой счет
            Account account = accountRepo.findByAccountNumber(data.getAccountNumber())
                .orElseGet(() -> {
                    log.debug("Лицевой счет не найден, создаем: {}", data.getAccountNumber());
                    return accountRepo.save(new Account(null, apartment.getId(), data.getAccountNumber(), data.getPayerName()));
                });
            log.trace("Счет: id={}, number={}", account.getId(), account.getAccountNumber());

            // 3. Сохраняем период начисления
            BillingPeriod billingPeriod = billingPeriodRepo.findByAccountIdAndPeriod(account.getId(), data.getBillingPeriod())
                .orElseGet(() -> {
                    log.debug("Период не найден, создаем: {} (accountId={})", data.getBillingPeriod(), account.getId());
                    return billingPeriodRepo.save(new BillingPeriod(null, account.getId(), data.getBillingPeriod(), data.getTotalAmount()));
                });
            log.trace("Период: id={}, period={}", billingPeriod.getId(), billingPeriod.getPeriod());

            // 4. Сохраняем начисления по приборам учета
            for (MeterCharge mc : data.getMeterCharges()) {
                String meter = mc.getMeterName();
                if (meter != null && !meter.isEmpty()) {
                    log.trace("Сохранение начисления по прибору: {}", meter);
                    meterChargeRepo.save(new MeterCharge(null, billingPeriod.getId(), meter, mc.getReading(), mc.getAmount()));
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при сохранении записи: {}", line.substring(0, Math.min(100, line.length())), e);
            throw new SQLException("Ошибка при сохранении адреса: " + e.getMessage(), e);
        }
    }

    /**
     * Извлечь тип ошибки БД из сообщения
     */
    private String extractDbErrorType(String errorMessage) {
        if (errorMessage == null) {
            return "Неизвестная ошибка";
        }

        if (errorMessage.contains("Невозможно сохранить невалидную запись")) {
            return "Невалидная запись после исправления";
        }

        if (errorMessage.contains("значение не умещается в тип")) {
            if (errorMessage.contains("character varying(20)")) {
                return "Превышение размера поля (VARCHAR(20))";
            } else if (errorMessage.contains("character varying")) {
                return "Превышение размера поля (VARCHAR)";
            }
        }

        if (errorMessage.contains("UNIQUE")) {
            return "Нарушение уникальности";
        }

        if (errorMessage.contains("foreign key")) {
            return "Нарушение внешнего ключа";
        }

        if (errorMessage.contains("null")) {
            return "NULL значение в обязательном поле";
        }

        if (errorMessage.contains("type") || errorMessage.contains("тип")) {
            return "Ошибка типа данных";
        }

        return "Другая ошибка: " + errorMessage.substring(0, Math.min(50, errorMessage.length()));
    }
}
