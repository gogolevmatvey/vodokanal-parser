package org.example;

import org.example.parser.model.ParseResult;
import org.example.parser.model.ParsedRecord;
import org.example.parser.model.Address;
import org.example.parser.model.Locality;
import org.example.parser.model.Street;
import org.example.parser.model.House;
import org.example.parser.model.Apartment;
import org.example.parser.model.Account;
import org.example.parser.model.BillingPeriod;
import org.example.parser.model.MeterCharge;
import org.example.service.RecordParser;
import org.example.service.RecordCorrector;
import org.example.service.SearchService;
import org.example.repository.impl.JdbcLocalityRepository;
import org.example.repository.impl.JdbcStreetRepository;
import org.example.repository.impl.JdbcHouseRepository;
import org.example.repository.impl.JdbcApartmentRepository;
import org.example.repository.impl.JdbcAccountRepository;
import org.example.repository.impl.JdbcBillingPeriodRepository;
import org.example.repository.impl.JdbcMeterChargeRepository;
import org.example.repository.LocalityRepository;
import org.example.repository.StreetRepository;
import org.example.repository.HouseRepository;
import org.example.repository.ApartmentRepository;
import org.example.repository.AccountRepository;
import org.example.repository.BillingPeriodRepository;
import org.example.repository.MeterChargeRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.sql.SQLException;

public class Main {

    private static final int PROGRESS_INTERVAL = 1000;
    private static final int DB_BATCH_SIZE = 500;

    public static void main(String[] args) {
        // Загружаем конфигурацию
        ConfigManager config = ConfigManager.getInstance();

        String filePath = config.getFileInput();
        String validFilePath = config.getFileOutputValid();
        String invalidFilePath = config.getFileOutputInvalid();
        String errorReportPath = config.getFileOutputErrorReport();
        String correctedFilePath = config.getFileOutputCorrected();
        String uncorrectedFilePath = config.getFileOutputUncorrected();

        // Создаем сервисы
        RecordParser parser = new RecordParser();
        RecordCorrector corrector = new RecordCorrector();

        int validCount = 0;
        int invalidCount = 0;
        int correctedCount = 0;
        int uncorrectedCount = 0;
        int totalCount = 0;
        int dbSavedCount = 0;
        int dbErrorCount = 0;

        // Карта для группировки ошибок по типам
        Map<String, Integer> errorCounts = new TreeMap<>();
        Map<String, List<String>> errorSamples = new HashMap<>();
        // Карта для подсчета исправленных ошибок по типам
        Map<String, Integer> correctedErrorCounts = new TreeMap<>();
        // Карта для группировки ошибок БД по типам
        Map<String, Integer> dbErrorCounts = new TreeMap<>();
        Map<String, List<String>> dbErrorSamples = new HashMap<>();

        // Подключение к базе данных
        System.out.println("Подключение к базе данных PostgreSQL...");
        try (DatabaseManager dbManager = new DatabaseManager()) {

            // Создаем репозитории
            LocalityRepository localityRepo = new JdbcLocalityRepository(dbManager.getDataSource());
            StreetRepository streetRepo = new JdbcStreetRepository(dbManager.getDataSource());
            HouseRepository houseRepo = new JdbcHouseRepository(dbManager.getDataSource());
            ApartmentRepository apartmentRepo = new JdbcApartmentRepository(dbManager.getDataSource());
            AccountRepository accountRepo = new JdbcAccountRepository(dbManager.getDataSource());
            BillingPeriodRepository billingPeriodRepo = new JdbcBillingPeriodRepository(dbManager.getDataSource());
            MeterChargeRepository meterChargeRepo = new JdbcMeterChargeRepository(dbManager.getDataSource());

            // Создаем сервисы
            SearchService searchService = new SearchService(localityRepo, streetRepo, houseRepo, apartmentRepo, accountRepo);

            System.out.println("База данных готова к работе.");
            System.out.println();

            // Счетчики для пакетной вставки в БД
            List<String> dbBatch = new ArrayList<>();
            
            try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(filePath),
                                Charset.forName("UTF-8")
                        )
                    );
                    BufferedWriter validWriter = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(validFilePath), Charset.forName("UTF-8"))
                    );
                    BufferedWriter invalidWriter = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(invalidFilePath), Charset.forName("UTF-8"))
                    );
                    BufferedWriter correctedWriter = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(correctedFilePath), Charset.forName("UTF-8"))
                    );
                    BufferedWriter uncorrectedWriter = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(uncorrectedFilePath), Charset.forName("UTF-8"))
                    )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    totalCount++;

                    ParseResult result = parser.parse(line);

                    if (result.isValid()) {
                        validCount++;
                        validWriter.write(line);
                        validWriter.newLine();

                        // Сохраняем в БД
                        dbBatch.add(line);
                        if (dbBatch.size() >= DB_BATCH_SIZE) {
                            int[] stats = saveToDatabaseWithStats(dbBatch, localityRepo, streetRepo, houseRepo, apartmentRepo, accountRepo, billingPeriodRepo, meterChargeRepo, dbErrorCounts, dbErrorSamples, parser);
                            dbSavedCount += stats[0];
                            dbErrorCount += stats[1];
                            dbBatch.clear();
                        }
                    } else {
                        invalidCount++;
                        invalidWriter.write(line + " | Ошибка: " + result.getErrorMessage());
                        invalidWriter.newLine();

                        // Исправляем запись и проверяем результат
                        String correctedLine = corrector.correct(line, result.getErrorMessage());
                        String errorType = extractErrorType(result.getErrorMessage());
                        boolean wasCorrected = isRecordCorrected(correctedLine, errorType);

                        if (wasCorrected) {
                            // Запись исправлена - пишем в corrected_records.txt
                            correctedWriter.write(correctedLine);
                            correctedWriter.newLine();
                            correctedCount++;
                            correctedErrorCounts.put(errorType, correctedErrorCounts.getOrDefault(errorType, 0) + 1);

                            // Сохраняем исправленную запись в БД
                            dbBatch.add(correctedLine);
                            if (dbBatch.size() >= DB_BATCH_SIZE) {
                                int[] stats = saveToDatabaseWithStats(dbBatch, localityRepo, streetRepo, houseRepo, apartmentRepo, accountRepo, billingPeriodRepo, meterChargeRepo, dbErrorCounts, dbErrorSamples, parser);
                                dbSavedCount += stats[0];
                                dbErrorCount += stats[1];
                                dbBatch.clear();
                            }
                        } else {
                            // Запись не удалось исправить - пишем в uncorrected_records.txt
                            uncorrectedWriter.write(line + " | Ошибка: " + result.getErrorMessage());
                            uncorrectedWriter.newLine();
                            uncorrectedCount++;
                        }

                        // Группировка ошибок по типу
                        errorCounts.put(errorType, errorCounts.getOrDefault(errorType, 0) + 1);

                        // Сохраняем примеры ошибок (до 5 на каждый тип)
                        errorSamples.computeIfAbsent(errorType, k -> new ArrayList<>());
                        if (errorSamples.get(errorType).size() < 5) {
                            errorSamples.get(errorType).add(line.substring(0, Math.min(150, line.length())));
                        }
                    }

                    if (totalCount % PROGRESS_INTERVAL == 0) {
                        System.out.printf("Обработано строк: %d, Валидных: %d, Ошибок: %d, Исправлено: %d, Не исправлено: %d, В БД: %d, Ошибок БД: %d%n",
                                totalCount, validCount, invalidCount, correctedCount, uncorrectedCount, dbSavedCount, dbErrorCount);
                    }
                }

                // Сохраняем остаток записей в БД
                if (!dbBatch.isEmpty()) {
                    int[] stats = saveToDatabaseWithStats(dbBatch, localityRepo, streetRepo, houseRepo, apartmentRepo, accountRepo, billingPeriodRepo, meterChargeRepo, dbErrorCounts, dbErrorSamples, parser);
                    dbSavedCount += stats[0];
                    dbErrorCount += stats[1];
                }
            } catch (IOException e) {
                System.err.println("Ошибка чтения файла: " + e.getMessage());
            }
        }

        // Запись отчета с группировкой ошибок
        writeErrorReport(errorReportPath, errorCounts, errorSamples, correctedErrorCounts, totalCount);

        // Вывод статистики
        long totalRecords = validCount + invalidCount;
        double validPercent = totalRecords > 0 ? (double) validCount / totalRecords * 100 : 0;
        double invalidPercent = totalRecords > 0 ? (double) invalidCount / totalRecords * 100 : 0;
        double correctedPercent = invalidCount > 0 ? (double) correctedCount / invalidCount * 100 : 0;
        double uncorrectedPercent = invalidCount > 0 ? (double) uncorrectedCount / invalidCount * 100 : 0;

        System.out.println();
        System.out.println("=== Результаты обработки ===");
        System.out.println("Всего обработано записей: " + totalRecords);
        System.out.println("Успешно обработано: " + validCount + " (" + String.format("%.2f", validPercent) + "%)");
        System.out.println("С ошибками: " + invalidCount + " (" + String.format("%.2f", invalidPercent) + "%)");
        System.out.println("Исправлено записей: " + correctedCount + " (" + String.format("%.2f", correctedPercent) + "% от ошибок)");
        System.out.println("Не исправлено записей: " + uncorrectedCount + " (" + String.format("%.2f", uncorrectedPercent) + "% от ошибок)");
        System.out.println();
        System.out.println("Валидные записи сохранены в файл: " + validFilePath);
        System.out.println("Записи с ошибками сохранены в файл: " + invalidFilePath);
        System.out.println("Исправленные записи сохранены в файл: " + correctedFilePath);
        System.out.println("Неисправленные ошибки сохранены в файл: " + uncorrectedFilePath);
        System.out.println();
        System.out.println("=== Статистика базы данных ===");
        System.out.println("Записей сохранено в базу данных: " + dbSavedCount);
        System.out.println("Ошибок при сохранении в БД: " + dbErrorCount);
        System.out.println();
        System.out.println("Баланс: " + totalRecords + " (всего) - " + uncorrectedCount + " (не исправлено) - " + dbErrorCount + " (ошибки БД) = " + (totalRecords - uncorrectedCount - dbErrorCount));
        
        // Вывод статистики по ошибкам БД
        if (!dbErrorCounts.isEmpty()) {
            System.out.println();
            System.out.println("=== Детальная статистика ошибок БД ===");
            System.out.println("Всего типов ошибок: " + dbErrorCounts.size());
            System.out.println();
            
            // Сортируем ошибки по количеству (убывание)
            List<Map.Entry<String, Integer>> sortedDbErrors = new ArrayList<>(dbErrorCounts.entrySet());
            sortedDbErrors.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            
            for (Map.Entry<String, Integer> entry : sortedDbErrors) {
                String errorType = entry.getKey();
                int count = entry.getValue();
                double percent = dbErrorCount > 0 ? (double) count / dbErrorCount * 100 : 0;
                
                System.out.printf("  %s: %d (%.2f%%)%n", errorType, count, percent);
                
                // Вывод примеров
                List<String> samples = dbErrorSamples.get(errorType);
                if (samples != null && !samples.isEmpty()) {
                    System.out.println("    Примеры:");
                    for (String sample : samples) {
                        System.out.printf("      - %s%n", sample);
                    }
                }
                System.out.println();
            }
        }
    }

    /**
     * Данные из валидной записи
     */
    private record RecordData(
            String accountNumber,
            String payerName,
            Map<String, Object> address,
            String billingPeriod,
            List<Map<String, Object>> charges
    ) {}

    /**
     * Проверяет, является ли значение допустимым для прибора учета или показаний
     */
    private static boolean isValidMeterOrReading(String value) {
        // Числа (суммы, показания) - поддерживаем форматы: 123, 123.45, 185.0000
        if (value.matches("^\\d+\\.?\\d*$") || value.matches("^\\d*\\.\\d+$")) {
            return true;
        }

        // Прибор учета - буквы (русские и латинские), цифры, спецсимволы
        // Поддерживаем текстовые обозначения: хол.вода, сан.узел, лет. до и т.д.
        if (value.matches("^[A-Za-zА-Яа-яЁё0-9№@_\\[?\\\\\\s\\-/,+().]+.$") || 
            value.matches("^[A-Za-zА-Яа-яЁё0-9№@_\\[?\\\\\\s\\-/,+().]+$")) {
            return true;
        }

        // Замаскированное значение
        if (value.equals(".*")) {
            return true;
        }

        return false;
    }

    /**
     * Проверяет, является ли строка текстовым месяцем
     */
    private static boolean isTextualMonth(String s) {
        return s.matches("^(янв|фев|мар|апр|май|июн|июл|авг|сен|окт|ноя|дек)\\.?\\d{2}$") ||
               s.matches("^\\d{2}\\.(янв|фев|мар|апр|май|июн|июл|авг|сен|окт|ноя|дек)$");
    }

    /**
     * Подсчитывает количество вхождений символа в строку
     */
    private static int countChar(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    /**
     * Извлекает тип ошибки из сообщения (первая часть до двоеточия или всё сообщение)
     */
    private static String extractErrorType(String errorMessage) {
        if (errorMessage.contains(":")) {
            return errorMessage.substring(0, errorMessage.indexOf(":")).trim();
        }
        return errorMessage.trim();
    }

    /**
     * Записывает отчет с группировкой ошибок по типам
     */
    private static void writeErrorReport(String filePath, Map<String, Integer> errorCounts,
                                         Map<String, List<String>> errorSamples,
                                         Map<String, Integer> correctedErrorCounts, int totalCount) {
        try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), Charset.forName("UTF-8"))
                )) {
            writer.write("================================================================================\n");
            writer.write("ОТЧЕТ ПО ОШИБКАМ\n");
            writer.write("================================================================================\n\n");

            int totalErrors = errorCounts.values().stream().mapToInt(Integer::intValue).sum();
            int totalCorrected = correctedErrorCounts.values().stream().mapToInt(Integer::intValue).sum();
            int totalUncorrected = totalErrors - totalCorrected;

            writer.write(String.format("Всего записей: %,d\n", totalCount));
            writer.write(String.format("Всего ошибок: %,d\n", totalErrors));
            writer.write(String.format("Исправлено ошибок: %,d (%.2f%%)\n", totalCorrected, (double) totalCorrected / totalCount * 100));
            writer.write(String.format("Не исправлено: %,d (%.2f%%)\n", totalUncorrected, (double) totalUncorrected / totalCount * 100));
            writer.write(String.format("Типов ошибок: %,d\n\n", errorCounts.size()));
            writer.write("Примечание: Некоторые ошибки (пустая часть адреса) не могут быть исправлены\n");
            writer.write("автоматически без потери данных или требуют ручного вмешательства.\n\n");

            writer.write("--------------------------------------------------------------------------------\n");
            writer.write("ГРУППИРОВКА ПО ТИПАМ ОШИБОК:\n");
            writer.write("--------------------------------------------------------------------------------\n\n");

            // Сортируем ошибки по количеству (убывание)
            List<Map.Entry<String, Integer>> sortedErrors = new ArrayList<>(errorCounts.entrySet());
            sortedErrors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<String, Integer> entry : sortedErrors) {
                String errorType = entry.getKey();
                int count = entry.getValue();
                int corrected = correctedErrorCounts.getOrDefault(errorType, 0);
                int uncorrected = count - corrected;
                double percent = (double) count / totalCount * 100;
                double correctedPercent = count > 0 ? (double) corrected / count * 100 : 0;

                writer.write(String.format("\n### %s: %,d (%.2f%%)\n", errorType, count, percent));
                writer.write(String.format("   Исправлено: %,d (%.2f%%)\n", corrected, correctedPercent));
                writer.write(String.format("   Не исправлено: %,d (%.2f%%)\n", uncorrected, 100 - correctedPercent));
                writer.write("   Примеры:\n");

                List<String> samples = errorSamples.get(errorType);
                if (samples != null) {
                    for (String sample : samples) {
                        writer.write(String.format("   - %s\n", sample));
                    }
                }
            }

            writer.write("\n================================================================================\n");
            writer.write("КОНЕЦ ОТЧЕТА\n");
            writer.write("================================================================================\n");
        } catch (IOException e) {
            System.err.println("Ошибка записи отчета: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Проверяет, была ли ошибка исправлена в записи
     */
    private static boolean isRecordCorrected(String correctedLine, String errorType) {
        // Проверяем, содержит ли исправленная запись признаки исправления
        switch (errorType) {
            case "Пустое ФИО плательщика":
                // Проверяем, что ФИО не пустое (исправлено или было не пустым)
                String[] nameParts = correctedLine.split(";");
                if (nameParts.length > 1) {
                    String correctedName = nameParts[1].trim();
                    // Исправлено, если ФИО не пустое
                    return !correctedName.isEmpty();
                }
                return false;
            case "Неполный адрес":
            case "Неполный адрес (меньше 3 запятых)":
                // Проверяем количество запятых в адресе (поле 3)
                String[] addrParts2 = correctedLine.split(";");
                if (addrParts2.length > 2) {
                    int commaCount = countChar(addrParts2[2], ',');
                    // Адрес исправлен, если теперь 3+ запятых или содержит "не указан"
                    return commaCount >= 3 || addrParts2[2].contains("не указан");
                }
                return false;
            case "Текстовый месяц в поле показаний":
                return !correctedLine.matches(".*;(янв|фев|мар|апр|май|июн|июл|авг|сен|окт|ноя|дек)\\.?\\d{2}.*");
            case "Неверный формат суммы начисления":
                String[] sumParts = correctedLine.split(";");
                if (sumParts.length > 4) {
                    try {
                        Double.parseDouble(sumParts[4].trim());
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                return false;
            case "Пустая часть адреса":
                // Сложно определить, оставим как неисправленное
                return false;
            case "Подозрительный адрес":
            case "Подозрительный адрес (больше 5 запятых)":
                // Слишком много запятых - исправляется объединением квартир
                String[] suspectAddrParts = correctedLine.split(";");
                if (suspectAddrParts.length > 2) {
                    int commaCount = countChar(suspectAddrParts[2], ',');
                    // Адрес исправлен, если теперь <= 5 запятых
                    return commaCount <= 5;
                }
                return false;
            case "Недостаточно полей в строке":
                return correctedLine.split(";").length >= 5;
            default:
                return false;
        }
    }

    /**
     * Извлекает тип ошибки БД из сообщения.
     */
    private static String extractDbErrorType(String errorMessage) {
        if (errorMessage == null) {
            return "Неизвестная ошибка";
        }
        
        // Невалидная запись
        if (errorMessage.contains("Невозможно сохранить невалидную запись")) {
            return "Невалидная запись после исправления";
        }
        
        // Ошибки размера поля
        if (errorMessage.contains("значение не умещается в тип")) {
            if (errorMessage.contains("character varying(20)")) {
                return "Превышение размера поля (VARCHAR(20))";
            } else if (errorMessage.contains("character varying")) {
                return "Превышение размера поля (VARCHAR)";
            }
        }
        
        // Ошибки уникальности
        if (errorMessage.contains("UNIQUE")) {
            return "Нарушение уникальности";
        }
        
        // Ошибки внешних ключей
        if (errorMessage.contains("foreign key")) {
            return "Нарушение внешнего ключа";
        }
        
        // Ошибки NULL
        if (errorMessage.contains("null")) {
            return "NULL значение в обязательном поле";
        }
        
        // Ошибки типа данных
        if (errorMessage.contains("type") || errorMessage.contains("тип")) {
            return "Ошибка типа данных";
        }
        
        // Всё остальное
        return "Другая ошибка: " + errorMessage.substring(0, Math.min(50, errorMessage.length()));
    }

    /**
     * Сохранение пакета записей в базу данных.
     * @return массив из двух чисел: [количество успешно сохраненных, количество ошибок]
     */
    private static int[] saveToDatabaseWithStats(List<String> records,
                                       LocalityRepository localityRepo,
                                       StreetRepository streetRepo,
                                       HouseRepository houseRepo,
                                       ApartmentRepository apartmentRepo,
                                       AccountRepository accountRepo,
                                       BillingPeriodRepository billingPeriodRepo,
                                       MeterChargeRepository meterChargeRepo,
                                       Map<String, Integer> dbErrorCounts,
                                       Map<String, List<String>> dbErrorSamples,
                                       RecordParser parser) {
        int savedCount = 0;
        int errorCount = 0;
        for (String line : records) {
            try {
                saveRecordToDatabase(line, localityRepo, streetRepo, houseRepo, apartmentRepo, accountRepo, billingPeriodRepo, meterChargeRepo, parser);
                savedCount++;
            } catch (Exception e) {
                errorCount++;
                String errorMsg = e.getMessage();

                // Извлекаем тип ошибки
                String errorType = extractDbErrorType(errorMsg);
                dbErrorCounts.put(errorType, dbErrorCounts.getOrDefault(errorType, 0) + 1);

                // Сохраняем примеры (до 5 на каждый тип)
                dbErrorSamples.computeIfAbsent(errorType, k -> new ArrayList<>());
                if (dbErrorSamples.get(errorType).size() < 5) {
                    dbErrorSamples.get(errorType).add(line.substring(0, Math.min(150, line.length())));
                }

                System.err.println("Ошибка сохранения записи в БД: " + errorMsg);
                System.err.println("Запись: " + line);
            }
        }
        if (errorCount > 0) {
            System.out.printf("   [БД] Сохранено: %d, Ошибок: %d%n", savedCount, errorCount);
        }
        return new int[]{savedCount, errorCount};
    }

    /**
     * Сохранение одной записи в базу данных.
     */
    private static void saveRecordToDatabase(String line,
                                              LocalityRepository localityRepo,
                                              StreetRepository streetRepo,
                                              HouseRepository houseRepo,
                                              ApartmentRepository apartmentRepo,
                                              AccountRepository accountRepo,
                                              BillingPeriodRepository billingPeriodRepo,
                                              MeterChargeRepository meterChargeRepo,
                                              RecordParser parser) throws SQLException {
        ParseResult result = parser.parse(line);
        if (!result.isValid()) {
            throw new SQLException("Невозможно сохранить невалидную запись");
        }

        ParsedRecord data = result.getRecordData();

        // 1. Сохраняем адрес (locality -> street -> house -> apartment)
        Address address = data.getAddress();
        String[] addressParts = address.getFullAddress().split(",");
        String localityName = addressParts[0].trim();
        String streetName = addressParts.length > 1 ? addressParts[1].trim() : "Улица не указана";
        String houseNumber = addressParts.length > 2 ? addressParts[2].trim() : "Дом не указан";
        String apartmentNumber = addressParts.length > 3 ? addressParts[3].trim() : "0";

        // Сохраняем или находим населенный пункт
        Locality locality = localityRepo.findByName(localityName)
            .orElseGet(() -> localityRepo.save(new Locality(null, localityName, null)));

        // Сохраняем или находим улицу
        Street street = streetRepo.findByLocalityIdAndName(locality.getId(), streetName)
            .orElseGet(() -> streetRepo.save(new Street(null, locality.getId(), streetName, null)));

        // Сохраняем или находим дом
        House house = houseRepo.findByStreetIdAndNumber(street.getId(), houseNumber)
            .orElseGet(() -> houseRepo.save(new House(null, street.getId(), houseNumber, null)));

        // Сохраняем или находим квартиру
        Apartment apartment = apartmentRepo.findByHouseIdAndNumber(house.getId(), apartmentNumber)
            .orElseGet(() -> apartmentRepo.save(new Apartment(null, house.getId(), apartmentNumber)));

        // 2. Сохраняем лицевой счет
        Account account = accountRepo.findByAccountNumber(data.getAccountNumber())
            .orElseGet(() -> accountRepo.save(new Account(null, apartment.getId(), data.getAccountNumber(), data.getPayerName())));

        // 3. Сохраняем период начисления
        BillingPeriod billingPeriod = billingPeriodRepo.findByAccountIdAndPeriod(account.getId(), data.getBillingPeriod())
            .orElseGet(() -> billingPeriodRepo.save(new BillingPeriod(null, account.getId(), data.getBillingPeriod(), data.getTotalAmount())));

        // 4. Сохраняем начисления по приборам учета
        for (MeterCharge mc : data.getMeterCharges()) {
            String meter = mc.getMeterName();
            if (meter != null && !meter.isEmpty()) {
                meterChargeRepo.save(new MeterCharge(null, billingPeriod.getId(), meter, mc.getReading(), mc.getAmount()));
            }
        }
    }
}
