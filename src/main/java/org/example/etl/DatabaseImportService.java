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

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Сервис для импорта данных в базу данных.
 * Использует batch-операции для accounts, billing periods и meter charges.
 */
@Service
public class DatabaseImportService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseImportService.class);

    private final DataSource dataSource;
    private final RecordParser parser;
    private final org.example.repository.LocalityRepository localityRepo;
    private final org.example.repository.StreetRepository streetRepo;
    private final org.example.repository.HouseRepository houseRepo;
    private final org.example.repository.ApartmentRepository apartmentRepo;

    // === Метрики производительности БД ===
    private final AtomicLong totalQueryTimeNs = new AtomicLong(0);
    private final AtomicInteger totalQueries = new AtomicInteger(0);

    // === In-memory кэш для адресных сущностей ===
    // Ключ: имя населённого пункта → ID
    private final Map<String, Long> localityCache = new ConcurrentHashMap<>();
    // Ключ: localityId + "::" + streetName → ID
    private final Map<String, Long> streetCache = new ConcurrentHashMap<>();
    // Ключ: streetId + "::" + houseNumber → ID
    private final Map<String, Long> houseCache = new ConcurrentHashMap<>();
    // Ключ: houseId + "::" + apartmentNumber → ID
    private final Map<String, Long> apartmentCache = new ConcurrentHashMap<>();

    // === Статистика кэша ===
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    /**
     * Размер чанка для IN-запросов
     */
    private static final int IN_CHUNK_SIZE = 1000;

    /**
     * Запись для batch-обработки
     */
    private static class BatchEntry {
        final String line;
        final ParsedRecord data;
        final String localityName;
        final String streetName;
        final String houseNumber;
        final String apartmentNumber;
        Long apartmentId;
        Long accountId;

        BatchEntry(String line, ParsedRecord data, String localityName,
                   String streetName, String houseNumber, String apartmentNumber) {
            this.line = line;
            this.data = data;
            this.localityName = localityName;
            this.streetName = streetName;
            this.houseNumber = houseNumber;
            this.apartmentNumber = apartmentNumber;
        }

        BatchEntry apartmentId(Long apartmentId) {
            this.apartmentId = apartmentId;
            return this;
        }
    }

    public DatabaseImportService(DataSource dataSource,
                                 RecordParser parser,
                                 org.example.repository.LocalityRepository localityRepo,
                                 org.example.repository.StreetRepository streetRepo,
                                 org.example.repository.HouseRepository houseRepo,
                                 org.example.repository.ApartmentRepository apartmentRepo) {
        this.dataSource = dataSource;
        this.parser = parser;
        this.localityRepo = localityRepo;
        this.streetRepo = streetRepo;
        this.houseRepo = houseRepo;
        this.apartmentRepo = apartmentRepo;
    }

    /**
     * Получить общее время запросов к БД (мс)
     */
    public long getTotalQueryTimeMs() {
        return totalQueryTimeNs.get() / 1_000_000;
    }

    /**
     * Получить общее количество запросов к БД
     */
    public int getTotalQueries() {
        return totalQueries.get();
    }

    // === Статистика кэша ===

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public long getCacheTotal() {
        return cacheHits.get() + cacheMisses.get();
    }

    public double getCacheHitRate() {
        long total = getCacheTotal();
        return total > 0 ? (double) cacheHits.get() / total * 100 : 0;
    }

    public int getLocalityCacheSize() {
        return localityCache.size();
    }

    public int getStreetCacheSize() {
        return streetCache.size();
    }

    public int getHouseCacheSize() {
        return houseCache.size();
    }

    public int getApartmentCacheSize() {
        return apartmentCache.size();
    }

    /**
     * Выполнить операцию с замером времени запроса к БД
     */
    private <T> T measureDbQuery(java.util.function.Supplier<T> operation) {
        long startTime = System.nanoTime();
        try {
            return operation.get();
        } finally {
            totalQueryTimeNs.addAndGet(System.nanoTime() - startTime);
            totalQueries.incrementAndGet();
        }
    }

    /**
     * Найти или создать населённый пункт (с кэшем)
     */
    private Locality getOrCreateLocality(String name) {
        // Проверяем кэш
        Long cachedId = localityCache.get(name);
        if (cachedId != null) {
            cacheHits.incrementAndGet();
            return new Locality(cachedId, name);
        }

        // Кэш промах — идём в БД
        cacheMisses.incrementAndGet();
        Locality locality = measureDbQuery(() ->
            localityRepo.findByName(name)
                .orElseGet(() -> {
                    log.debug("Населенный пункт не найден, создаем: {}", name);
                    return localityRepo.save(new Locality(null, name));
                })
        );

        // Сохраняем результат в кэш
        localityCache.put(name, locality.getId());
        return locality;
    }

    /**
     * Найти или создать улицу (с кэшем)
     */
    private Street getOrCreateStreet(Long localityId, String name) {
        String cacheKey = localityId + "::" + name;

        // Проверяем кэш
        Long cachedId = streetCache.get(cacheKey);
        if (cachedId != null) {
            cacheHits.incrementAndGet();
            return new Street(cachedId, localityId, name, null);
        }

        // Кэш промах — идём в БД
        cacheMisses.incrementAndGet();
        Street street = measureDbQuery(() ->
            streetRepo.findByLocalityIdAndName(localityId, name)
                .orElseGet(() -> {
                    log.debug("Улица не найдена, создаем: {} (localityId={})", name, localityId);
                    return streetRepo.save(new Street(null, localityId, name, null));
                })
        );

        // Сохраняем результат в кэш
        streetCache.put(cacheKey, street.getId());
        return street;
    }

    /**
     * Найти или создать дом (с кэшем)
     */
    private House getOrCreateHouse(Long streetId, String number) {
        String cacheKey = streetId + "::" + number;

        // Проверяем кэш
        Long cachedId = houseCache.get(cacheKey);
        if (cachedId != null) {
            cacheHits.incrementAndGet();
            return new House(cachedId, streetId, number, null);
        }

        // Кэш промах — идём в БД
        cacheMisses.incrementAndGet();
        House house = measureDbQuery(() ->
            houseRepo.findByStreetIdAndNumber(streetId, number)
                .orElseGet(() -> {
                    log.debug("Дом не найден, создаем: {} (streetId={})", number, streetId);
                    return houseRepo.save(new House(null, streetId, number, null));
                })
        );

        // Сохраняем результат в кэш
        houseCache.put(cacheKey, house.getId());
        return house;
    }

    /**
     * Найти или создать квартиру (с кэшем)
     */
    private Apartment getOrCreateApartment(Long houseId, String number) {
        String cacheKey = houseId + "::" + number;

        // Проверяем кэш
        Long cachedId = apartmentCache.get(cacheKey);
        if (cachedId != null) {
            cacheHits.incrementAndGet();
            return new Apartment(cachedId, houseId, number);
        }

        // Кэш промах — идём в БД
        cacheMisses.incrementAndGet();
        Apartment apartment = measureDbQuery(() ->
            apartmentRepo.findByHouseIdAndNumber(houseId, number)
                .orElseGet(() -> {
                    log.debug("Квартира не найдена, создаем: {} (houseId={})", number, houseId);
                    return apartmentRepo.save(new Apartment(null, houseId, number));
                })
        );

        // Сохраняем результат в кэш
        apartmentCache.put(cacheKey, apartment.getId());
        return apartment;
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
     * Сохранить пакет записей в базу данных.
     * Использует batch-операции для accounts, billing periods и meter charges.
     */
    public BatchResult saveBatch(List<String> records, int maxErrorSamples) {
        long batchStart = System.nanoTime();
        int errorCount = 0;
        Map<String, Integer> errorCounts = new TreeMap<>();
        Map<String, List<String>> errorSamples = new HashMap<>();

        // Phase 1: Парсинг и разрешение адресов (с кэшем)
        List<BatchEntry> entries = new ArrayList<>();
        for (String line : records) {
            try {
                entries.add(parseAndResolveAddress(line));
            } catch (Exception e) {
                errorCount++;
                String errorType = extractDbErrorType(e.getMessage());
                errorCounts.merge(errorType, 1, Integer::sum);
                List<String> samples = errorSamples.computeIfAbsent(errorType, k -> new ArrayList<>());
                if (samples.size() < maxErrorSamples) {
                    samples.add(line.substring(0, Math.min(150, line.length())));
                }
            }
        }

        if (entries.isEmpty()) {
            totalQueryTimeNs.addAndGet(System.nanoTime() - batchStart);
            totalQueries.incrementAndGet();
            return new BatchResult(0, errorCount, errorCounts, errorSamples);
        }

        // Phase 2-4: Batch-операции в одной транзакции
        try {
            batchSaveAccounts(entries);
            batchSaveBillingPeriods(entries);
            batchSaveMeterCharges(entries);
        } catch (Exception e) {
            // Batch-операция провалилась — помечаем все записи как ошибки
            errorCount += entries.size();
            String errorType = extractDbErrorType(e.getMessage());
            errorCounts.merge(errorType, entries.size(), Integer::sum);
            List<String> samples = errorSamples.computeIfAbsent(errorType, k -> new ArrayList<>());
            if (samples.isEmpty()) {
                String msg = e.getMessage();
                samples.add(msg != null ? msg.substring(0, Math.min(150, msg.length())) : "unknown");
            }
            entries.clear();
        }

        int savedCount = entries.size();
        totalQueryTimeNs.addAndGet(System.nanoTime() - batchStart);
        totalQueries.incrementAndGet();
        return new BatchResult(savedCount, errorCount, errorCounts, errorSamples);
    }

    /**
     * Распарсить строку и разрешить адресные сущности через кэш
     */
    private BatchEntry parseAndResolveAddress(String line) throws SQLException {
        ParseResult result = parser.parse(line);
        if (!result.isValid()) {
            throw new SQLException("Невозможно сохранить невалидную запись: " + result.getErrorMessage());
        }

        ParsedRecord data = result.getRecordData();
        Address address = data.getAddress();
        String[] addressParts = address.getFullAddress().split(",");
        String localityName = addressParts[0].trim();
        String streetName = addressParts.length > 1 ? addressParts[1].trim() : "Улица не указана";
        String houseNumber = addressParts.length > 2 ? addressParts[2].trim() : "Дом не указан";
        String apartmentNumber = addressParts.length > 3 && !addressParts[3].trim().isEmpty()
            ? addressParts[3].trim()
            : "данные отсутствуют";

        // Разрешаем адрес через кэш
        Locality locality = getOrCreateLocality(localityName);
        Street street = getOrCreateStreet(locality.getId(), streetName);
        House house = getOrCreateHouse(street.getId(), houseNumber);
        Apartment apartment = getOrCreateApartment(house.getId(), apartmentNumber);

        return new BatchEntry(line, data, localityName, streetName, houseNumber, apartmentNumber)
            .apartmentId(apartment.getId());
    }

    /**
     * Batch-операция: найти или создать accounts
     */
    private void batchSaveAccounts(List<BatchEntry> entries) throws SQLException {
        // Собираем уникальные номера счетов
        Map<String, BatchEntry> byAccountNumber = new LinkedHashMap<>();
        for (BatchEntry e : entries) {
            byAccountNumber.putIfAbsent(e.data.getAccountNumber(), e);
        }

        List<String> accountNumbers = new ArrayList<>(byAccountNumber.keySet());

        // Находим существующие счета
        Map<String, Long> existingIds = findExistingAccounts(accountNumbers);

        // Разделяем на существующие и новые
        List<BatchEntry> newEntries = new ArrayList<>();
        for (Map.Entry<String, BatchEntry> me : byAccountNumber.entrySet()) {
            String accNum = me.getKey();
            BatchEntry entry = me.getValue();
            Long existingId = existingIds.get(accNum);
            if (existingId != null) {
                entry.accountId = existingId;
            } else {
                newEntries.add(entry);
            }
        }

        // Batch-вставка новых счетов
        if (!newEntries.isEmpty()) {
            batchInsertAccounts(newEntries);
            // Перенаходим, чтобы получить ID новых записей
            List<String> newNumbers = new ArrayList<>();
            for (BatchEntry e : newEntries) {
                newNumbers.add(e.data.getAccountNumber());
            }
            Map<String, Long> newlyInserted = findExistingAccounts(newNumbers);
            for (BatchEntry e : newEntries) {
                e.accountId = newlyInserted.get(e.data.getAccountNumber());
            }
        }

        // Мапим accountId на все записи
        for (BatchEntry entry : entries) {
            if (entry.accountId == null) {
                entry.accountId = existingIds.get(entry.data.getAccountNumber());
            }
        }
    }

    /**
     * Найти существующие счета по номерам (с чанкованием IN-запроса)
     */
    private Map<String, Long> findExistingAccounts(List<String> accountNumbers) throws SQLException {
        Map<String, Long> result = new HashMap<>();
        if (accountNumbers.isEmpty()) return result;

        try (Connection conn = dataSource.getConnection()) {
            for (int i = 0; i < accountNumbers.size(); i += IN_CHUNK_SIZE) {
                int end = Math.min(i + IN_CHUNK_SIZE, accountNumbers.size());
                List<String> chunk = accountNumbers.subList(i, end);
                String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
                String sql = "SELECT id, account_number FROM accounts WHERE account_number IN (" + placeholders + ")";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (int j = 0; j < chunk.size(); j++) {
                        stmt.setString(j + 1, chunk.get(j));
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            result.put(rs.getString("account_number"), rs.getLong("id"));
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Batch-вставка новых счетов с ON CONFLICT
     */
    private void batchInsertAccounts(List<BatchEntry> entries) throws SQLException {
        String sql = "INSERT INTO accounts (apartment_id, account_number, payer_name) VALUES (?, ?, ?) " +
                     "ON CONFLICT (account_number) DO UPDATE SET payer_name = EXCLUDED.payer_name";
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (BatchEntry entry : entries) {
                    stmt.setLong(1, entry.apartmentId);
                    stmt.setString(2, entry.data.getAccountNumber());
                    stmt.setString(3, entry.data.getPayerName());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
    }

    /**
     * Batch-операция: найти или создать billing periods
     */
    private void batchSaveBillingPeriods(List<BatchEntry> entries) throws SQLException {
        // Собираем уникальные (accountId, period)
        Map<String, BatchEntry> byKey = new LinkedHashMap<>();
        for (BatchEntry e : entries) {
            String key = e.accountId + "::" + e.data.getBillingPeriod();
            byKey.putIfAbsent(key, e);
        }

        // Находим существующие периоды
        Map<String, Long> existingIds = findExistingBillingPeriods(byKey.keySet());

        // Определяем новые периоды
        List<Map.Entry<String, BatchEntry>> newEntries = new ArrayList<>();
        for (Map.Entry<String, BatchEntry> me : byKey.entrySet()) {
            String key = me.getKey();
            if (!existingIds.containsKey(key)) {
                newEntries.add(me);
            }
        }

        // Batch-вставка новых периодов
        if (!newEntries.isEmpty()) {
            batchInsertBillingPeriods(newEntries);
        }
    }

    /**
     * Найти существующие billing periods по ключам (accountId::period)
     */
    private Map<String, Long> findExistingBillingPeriods(Collection<String> keys) throws SQLException {
        Map<String, Long> result = new HashMap<>();
        if (keys.isEmpty()) return result;

        // Разбиваем ключи на accountId и period
        Map<Long, List<String>> byAccountId = new HashMap<>();
        Map<String, String> keyToPeriod = new HashMap<>();
        for (String key : keys) {
            String[] parts = key.split("::", 2);
            Long accountId = Long.parseLong(parts[0]);
            String period = parts[1];
            byAccountId.computeIfAbsent(accountId, k -> new ArrayList<>()).add(period);
            keyToPeriod.put(key, period);
        }

        try (Connection conn = dataSource.getConnection()) {
            for (Map.Entry<Long, List<String>> me : byAccountId.entrySet()) {
                Long accountId = me.getKey();
                List<String> periods = me.getValue();

                for (int i = 0; i < periods.size(); i += IN_CHUNK_SIZE) {
                    int end = Math.min(i + IN_CHUNK_SIZE, periods.size());
                    List<String> chunk = periods.subList(i, end);
                    String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
                    String sql = "SELECT id, period FROM billing_periods WHERE account_id = ? AND period IN (" + placeholders + ")";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setLong(1, accountId);
                        for (int j = 0; j < chunk.size(); j++) {
                            stmt.setString(j + 2, chunk.get(j));
                        }
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                String period = rs.getString("period");
                                String key = accountId + "::" + period;
                                result.put(key, rs.getLong("id"));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Batch-вставка новых billing periods с ON CONFLICT
     */
    private void batchInsertBillingPeriods(List<Map.Entry<String, BatchEntry>> entries) throws SQLException {
        String sql = "INSERT INTO billing_periods (account_id, period, total_amount) VALUES (?, ?, ?) " +
                     "ON CONFLICT (account_id, period) DO UPDATE SET total_amount = EXCLUDED.total_amount";
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map.Entry<String, BatchEntry> me : entries) {
                    BatchEntry entry = me.getValue();
                    stmt.setLong(1, entry.accountId);
                    stmt.setString(2, entry.data.getBillingPeriod());
                    stmt.setBigDecimal(3, entry.data.getTotalAmount());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
    }

    /**
     * Batch-вставка meter charges
     */
    private void batchSaveMeterCharges(List<BatchEntry> entries) throws SQLException {
        // Собираем все meter charges
        List<MeterChargeData> charges = new ArrayList<>();
        for (BatchEntry entry : entries) {
            for (MeterCharge mc : entry.data.getMeterCharges()) {
                String meter = mc.getMeterName();
                if (meter != null && !meter.isEmpty()) {
                    charges.add(new MeterChargeData(
                        entry.accountId,
                        entry.data.getBillingPeriod(),
                        meter,
                        mc.getReading(),
                        mc.getAmount()
                    ));
                }
            }
        }

        if (charges.isEmpty()) return;

        String sql = "INSERT INTO meter_charges (account_id, period, meter_name, reading, amount) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT (account_id, period, meter_name) DO UPDATE " +
                     "SET reading = EXCLUDED.reading, amount = EXCLUDED.amount";
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (MeterChargeData mc : charges) {
                    stmt.setLong(1, mc.accountId);
                    stmt.setString(2, mc.period);
                    stmt.setString(3, mc.meterName);
                    if (mc.reading != null) {
                        stmt.setBigDecimal(4, mc.reading);
                    } else {
                        stmt.setNull(4, Types.DECIMAL);
                    }
                    stmt.setBigDecimal(5, mc.amount);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
    }

    /**
     * Вспомогательный класс для данных meter charge
     */
    private static class MeterChargeData {
        final Long accountId;
        final String period;
        final String meterName;
        final BigDecimal reading;
        final BigDecimal amount;

        MeterChargeData(Long accountId, String period, String meterName,
                        BigDecimal reading, BigDecimal amount) {
            this.accountId = accountId;
            this.period = period;
            this.meterName = meterName;
            this.reading = reading;
            this.amount = amount;
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
