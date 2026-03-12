package org.example;

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

public class Main {

    private static final int PROGRESS_INTERVAL = 1000;

    public static void main(String[] args) {
        String filePath = "Testovye_dannye (1).txt";
        String validFilePath = "valid_records.txt";
        String invalidFilePath = "invalid_records.txt";
        String errorReportPath = "error_report.txt";
        String correctedFilePath = "corrected_records.txt";
        String uncorrectedFilePath = "uncorrected_records.txt";

        int validCount = 0;
        int invalidCount = 0;
        int correctedCount = 0;
        int uncorrectedCount = 0;
        int totalCount = 0;

        // Карта для группировки ошибок по типам
        Map<String, Integer> errorCounts = new TreeMap<>();
        Map<String, List<String>> errorSamples = new HashMap<>();
        // Карта для подсчета исправленных ошибок по типам
        Map<String, Integer> correctedErrorCounts = new TreeMap<>();

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

                ParseResult result = parseRecord(line);

                if (result.isValid()) {
                    validCount++;
                    validWriter.write(line);
                    validWriter.newLine();
                } else {
                    invalidCount++;
                    invalidWriter.write(line + " | Ошибка: " + result.errorMessage());
                    invalidWriter.newLine();

                    // Исправляем запись и проверяем результат
                    String correctedLine = correctRecord(line, result.errorMessage());
                    String errorType = extractErrorType(result.errorMessage());
                    boolean wasCorrected = isRecordCorrected(correctedLine, errorType);

                    if (wasCorrected) {
                        // Запись исправлена - пишем в corrected_records.txt
                        correctedWriter.write(correctedLine);
                        correctedWriter.newLine();
                        correctedCount++;
                        correctedErrorCounts.put(errorType, correctedErrorCounts.getOrDefault(errorType, 0) + 1);
                    } else {
                        // Запись не удалось исправить - пишем в uncorrected_records.txt
                        uncorrectedWriter.write(line + " | Ошибка: " + result.errorMessage());
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
                    System.out.printf("Обработано строк: %d, Валидных: %d, Ошибок: %d, Исправлено: %d, Не исправлено: %d%n",
                            totalCount, validCount, invalidCount, correctedCount, uncorrectedCount);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        }

        // Запись отчета с группировкой ошибок
        writeErrorReport(errorReportPath, errorCounts, errorSamples, correctedErrorCounts, totalCount);

        // Вывод статистики
        long totalRecords = validCount + invalidCount;
        double validPercent = totalRecords > 0 ? (double) validCount / totalRecords * 100 : 0;
        double invalidPercent = totalRecords > 0 ? (double) invalidCount / totalRecords * 100 : 0;
        double correctedPercent = invalidCount > 0 ? (double) correctedCount / invalidCount * 100 : 0;
        double uncorrectedPercent = invalidCount > 0 ? (double) uncorrectedCount / invalidCount * 100 : 0;

        System.out.println("=== Результаты обработки ===");
        System.out.println("Всего обработано записей: " + totalRecords);
        System.out.println("Успешно обработано: " + validCount + " (" + String.format("%.2f", validPercent) + "%)");
        System.out.println("С ошибками: " + invalidCount + " (" + String.format("%.2f", invalidPercent) + "%)");
        System.out.println("Исправлено записей: " + correctedCount + " (" + String.format("%.2f", correctedPercent) + "% от ошибок)");
        System.out.println("Не исправлено записей: " + uncorrectedCount + " (" + String.format("%.2f", uncorrectedPercent) + "% от ошибок)");
        System.out.println("Валидные записи сохранены в файл: " + validFilePath);
        System.out.println("Записи с ошибками сохранены в файл: " + invalidFilePath);
        System.out.println("Исправленные записи сохранены в файл: " + correctedFilePath);
        System.out.println("Неисправленные ошибки сохранены в файл: " + uncorrectedFilePath);
    }

    /**
     * Результат парсинга записи
     */
    private record ParseResult(boolean isValid, String errorMessage, RecordData recordData) {}

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
     * Парсит одну строку данных
     */
    private static ParseResult parseRecord(String line) {
        try {

            String[] parts = line.split(";");

            // Проверяем минимальное количество частей
            if (parts.length < 5) {
                return new ParseResult(false, "Недостаточно полей в строке", null);
            }

            // 1. Номер лицевого счета
            String accountNumber = parts[0].trim();
            if (accountNumber.isEmpty()) {
                return new ParseResult(false, "Пустой номер лицевого счета", null);
            }
            if (!accountNumber.matches("^\\d+$")) {
                return new ParseResult(false, "Неверный формат номера лицевого счета", null);
            }

            // 2. ФИО плательщика - должно быть обязательно
            String payerName = parts[1].trim();
            if (payerName.isEmpty() || payerName.matches("\\s*") || payerName.equals(".*") || payerName.matches("^[.\\*\\s\\d]+$")) {
                return new ParseResult(false, "Пустое ФИО плательщика", null);
            }

            // 3. Адрес (разделенный запятыми)
            String addressStr = parts[2].trim();
            if (addressStr.isEmpty()) {
                return new ParseResult(false, "Пустой адрес", null);
            }

            // Проверка на минимальное количество запятых (минимум 3)
            int commaCount = countChar(addressStr, ',');
            if (commaCount < 3) {
                return new ParseResult(false, "Неполный адрес (меньше 3 запятых)", null);
            }

            // Проверка на подозрительный адрес (больше 5 запятых)
            if (commaCount > 5) {
                return new ParseResult(false, "Подозрительный адрес (больше 5 запятых)", null);
            }

            String[] addressParts = addressStr.split(",");

            // Проверка на пустые части адреса
            for (int i = 0; i < Math.min(3, addressParts.length); i++) {
                if (addressParts[i].trim().isEmpty()) {
                    return new ParseResult(false, "Пустая часть адреса", null);
                }
            }

            String locality = addressParts[0].trim();
            String street = addressParts[1].trim();
            String house = addressParts[2].trim();

            // Квартиры (если есть) - начиная с 4-й части адреса
            List<String> apartments = new ArrayList<>();
            for (int i = 3; i < addressParts.length; i++) {
                String apt = addressParts[i].trim();
                if (!apt.isEmpty()) {
                    apartments.add(apt);
                }
            }

            Map<String, Object> addressDoc = new HashMap<>();
            addressDoc.put("locality", locality);
            addressDoc.put("street", street);
            addressDoc.put("house", house);
            addressDoc.put("apartments", apartments);

            // 4. Период начисления
            String billingPeriod = parts[3].trim();
            if (billingPeriod.isEmpty()) {
                return new ParseResult(false, "Пустой период начисления", null);
            }
            // Проверка формата периода (число или текстовый месяц)
            if (!billingPeriod.matches("^\\d{2,4}$") && isTextualMonth(billingPeriod)) {
                return new ParseResult(false, "Период в текстовом формате", null);
            }

            // 5. Суммы начисления, приборы учета и показания
            List<Map<String, Object>> charges = new ArrayList<>();

            // Если в строке 5 частей - это простой случай (только сумма)
            if (parts.length == 5) {
                try {
                    double amount = Double.parseDouble(parts[4].trim());
                    Map<String, Object> charge = new HashMap<>();
                    charge.put("amount", amount);
                    charge.put("meter", null);
                    charge.put("meterReading", null);
                    charges.add(charge);
                } catch (NumberFormatException e) {
                    return new ParseResult(false, "Неверный формат суммы начисления: " + parts[4], null);
                }
            }
            // Иначе обрабатываем: первая сумма, затем пары (прибор, показания)
            else {
                try {
                    // Первое поле после периода - это сумма
                    double amount = Double.parseDouble(parts[4].trim());

                    // Собираем все приборы и показания
                    List<String> meters = new ArrayList<>();
                    List<Double> readings = new ArrayList<>();

                    for (int i = 5; i < parts.length; i += 2) {
                        String meter = parts[i].trim();
                        if (meter.isEmpty()) {
                            meter = null;
                        } else if (!isValidMeterOrReading(meter)) {
                            // Проверяем, не является ли это текстовым месяцем
                            if (isTextualMonth(meter)) {
                                return new ParseResult(false, "Текстовый месяц в поле данных: " + meter, null);
                            }
                            // Подозрительное значение прибора
                            return new ParseResult(false, "Подозрительное значение прибора учета: " + meter, null);
                        }

                        Double reading = null;
                        if (i + 1 < parts.length) {
                            String readingStr = parts[i + 1].trim();
                            if (!readingStr.isEmpty()) {
                                // Проверяем, не является ли показание текстовым месяцем
                                if (isTextualMonth(readingStr)) {
                                    return new ParseResult(false, "Текстовый месяц в поле показаний: " + readingStr, null);
                                }
                                if (!isValidMeterOrReading(readingStr)) {
                                    return new ParseResult(false, "Неверный формат показаний: " + readingStr, null);
                                }
                                try {
                                    reading = Double.parseDouble(readingStr);
                                } catch (NumberFormatException e) {
                                    return new ParseResult(false, "Неверный формат показаний: " + readingStr, null);
                                }
                            }
                        }

                        meters.add(meter);
                        readings.add(reading);
                    }

                    // Если есть приборы - создаем запись для каждого
                    if (!meters.isEmpty()) {
                        for (int i = 0; i < meters.size(); i++) {
                            Map<String, Object> charge = new HashMap<>();
                            charge.put("amount", amount);
                            charge.put("meter", meters.get(i));
                            charge.put("meterReading", readings.get(i));
                            charges.add(charge);
                        }
                    } else {
                        // Нет приборов - просто сумма
                        Map<String, Object> charge = new HashMap<>();
                        charge.put("amount", amount);
                        charge.put("meter", null);
                        charge.put("meterReading", null);
                        charges.add(charge);
                    }
                } catch (NumberFormatException e) {
                    return new ParseResult(false, "Неверный формат суммы начисления: " + parts[4], null);
                }
            }

            RecordData recordData = new RecordData(
                    accountNumber,
                    payerName,
                    addressDoc,
                    billingPeriod,
                    charges
            );

            return new ParseResult(true, null, recordData);

        } catch (Exception e) {
            return new ParseResult(false, "Ошибка парсинга строки: " + e.getMessage(), null);
        }
    }

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
            writer.write("Примечание: Некоторые ошибки (неполный адрес, пустая часть адреса, подозрительный адрес)\n");
            writer.write("не могут быть исправлены автоматически без потери данных или требуют ручного вмешательства.\n\n");

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
                return correctedLine.contains("данные отсутствуют");
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
                // Слишком много запятых - исправляется слиянием
                String[] suspectAddrParts = correctedLine.split(";");
                if (suspectAddrParts.length > 2) {
                    int commaCount = countChar(suspectAddrParts[2], ',');
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
     * Исправляет ошибочную запись согласно правилам из zadanie.txt
     */
    private static String correctRecord(String line, String errorMessage) {
        String[] parts = line.split(";");

        // Если недостаточно полей - пытаемся восстановить
        if (parts.length < 5) {
            // Добавляем недостающие поля
            while (parts.length < 5) {
                String[] newParts = new String[parts.length + 1];
                System.arraycopy(parts, 0, newParts, 0, parts.length);
                newParts[parts.length] = "";
                parts = newParts;
            }
        }

        // 1. Исправление ФИО (если пустое или ".*")
        String payerName = parts[1].trim();
        if (payerName.isEmpty() || payerName.matches("^[.\\*\\s]+$") || payerName.matches("^[\\d\\s]+$")) {
            parts[1] = "данные отсутствуют";
        }

        // 2. Исправление адреса
        if (parts.length > 2) {
            String addressStr = parts[2].trim();
            String[] addressParts = addressStr.split(",");
            int commaCount = countChar(addressStr, ',');

            // Если меньше 3 запятых - исправляем, добавляя недостающие части
            if (commaCount < 3) {
                String locality = addressParts.length > 0 ? addressParts[0].trim() : "Населенный пункт не указан";
                String street = addressParts.length > 1 ? addressParts[1].trim() : "Улица не указана";
                String house = addressParts.length > 2 ? addressParts[2].trim() : "Дом не указан";

                // Если всего 1 часть (0 запятых) - это населенный пункт
                if (commaCount == 0) {
                    parts[2] = locality + ", Улица не указана, Дом не указан, 0";
                }
                // Если 2 запятые (1 часть) - это населенный пункт и улица
                else if (commaCount == 1) {
                    parts[2] = locality + ", " + street + ", Дом не указан, 0";
                }
                // Если 2 запятые - это населенный пункт, улица и дом, но нет квартиры
                else if (commaCount == 2) {
                    parts[2] = locality + ", " + street + ", " + house + ", 0";
                }
            }
            // Если больше 3 запятых (больше 4 частей) - сливаем лишние
            else if (addressParts.length > 4) {
                // Сливаем первые поля (населенный пункт + микрорайон/доп.информация)
                StringBuilder localityBuilder = new StringBuilder(addressParts[0].trim());
                for (int i = 1; i <= addressParts.length - 3; i++) {
                    localityBuilder.append(", ").append(addressParts[i].trim());
                }

                // Последние 3 части: улица, дом, квартира
                String street = addressParts[addressParts.length - 3].trim();
                String house = addressParts[addressParts.length - 2].trim();
                String apartment = addressParts[addressParts.length - 1].trim();

                parts[2] = localityBuilder.toString() + ", " + street + ", " + house + ", " + apartment;
            }
            // Если после дома несколько полей (4 части всего) - сливаем в одно
            else if (addressParts.length == 4) {
                // Проверяем, не является ли 4-я часть продолжением адреса
                String part4 = addressParts[3].trim();
                if (!part4.isEmpty() && !part4.matches("^\\d+$")) {
                    // Сливаем квартиру с дополнительными полями
                    StringBuilder apartmentBuilder = new StringBuilder(addressParts[2].trim());
                    for (int i = 3; i < addressParts.length; i++) {
                        apartmentBuilder.append(" ").append(addressParts[i].trim());
                    }
                    parts[2] = addressParts[0].trim() + ", " + addressParts[1].trim() + ", " +
                               addressParts[2].trim() + ", " + apartmentBuilder.toString().substring(addressParts[2].trim().length()).trim();
                }
            }
        }

        // 3. Исправление периода начисления
        if (parts.length > 3) {
            String billingPeriod = parts[3].trim();
            if (billingPeriod.isEmpty() || isTextualMonth(billingPeriod) || !billingPeriod.matches("^\\d+$")) {
                parts[3] = "0";
            }
        }

        // 4. Исправление суммы начисления
        if (parts.length > 4) {
            String amountStr = parts[4].trim();
            if (amountStr.isEmpty() || isTextualMonth(amountStr)) {
                parts[4] = "0";
            } else {
                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount < 0) {
                        // Отрицательная сумма - это перерасчет, оставляем как есть
                    }
                } catch (NumberFormatException e) {
                    parts[4] = "0";
                }
            }
        }

        // 5. Исправление приборов учета и показаний
        for (int i = 5; i < parts.length; i += 2) {
            String meter = parts[i].trim();
            String reading = (i + 1 < parts.length) ? parts[i + 1].trim() : "";

            // Исправление показаний
            if (!reading.isEmpty()) {
                if (isTextualMonth(reading)) {
                    // Текстовый месяц в показаниях - заменяем на 0
                    parts[i + 1] = "0";
                } else if (!isValidMeterOrReading(reading)) {
                    // Неверный формат показаний - заменяем на 0
                    parts[i + 1] = "0";
                }
            }
        }

        // Собираем исправленную строку
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(";").append(parts[i]);
        }

        return result.toString();
    }
}
