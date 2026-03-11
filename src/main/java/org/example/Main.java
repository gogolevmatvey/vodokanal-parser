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

public class Main {

    private static final int PROGRESS_INTERVAL = 1000;

    public static void main(String[] args) {
        String filePath = "Testovye_dannye (1).txt";
        String validFilePath = "valid_records.txt";
        String invalidFilePath = "invalid_records.txt";

        int validCount = 0;
        int invalidCount = 0;
        int totalCount = 0;

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
                }

                if (totalCount % PROGRESS_INTERVAL == 0) {
                    System.out.printf("Обработано строк: %d, Валидных: %d, Ошибок: %d%n",
                            totalCount, validCount, invalidCount);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        }

        // Вывод статистики
        long totalRecords = validCount + invalidCount;
        double validPercent = totalRecords > 0 ? (double) validCount / totalRecords * 100 : 0;
        double invalidPercent = totalRecords > 0 ? (double) invalidCount / totalRecords * 100 : 0;

        System.out.println("=== Результаты обработки ===");
        System.out.println("Всего обработано записей: " + totalRecords);
        System.out.println("Успешно обработано: " + validCount + " (" + String.format("%.2f", validPercent) + "%)");
        System.out.println("С ошибками: " + invalidCount + " (" + String.format("%.2f", invalidPercent) + "%)");
        System.out.println("Валидные записи сохранены в файл: " + validFilePath);
        System.out.println("Записи с ошибками сохранены в файл: " + invalidFilePath);
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

            // 2. ФИО плательщика - должно быть обязательно
            String payerName = parts[1].trim();
            if (payerName.isEmpty() || payerName.matches("\\s*") || payerName.contains(",") || payerName.matches("^[.\\*\\s\\d]+$")) {
                return new ParseResult(false, "Пустое ФИО плательщика", null);
            }

            // 3. Адрес (разделенный запятыми)
            String addressStr = parts[2].trim();
            if (addressStr.isEmpty()) {
                return new ParseResult(false, "Пустой адрес", null);
            }
            
            String[] addressParts = addressStr.split(",");

            if (addressParts.length < 3) {
                return new ParseResult(false, "Неполный адрес", null);
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
                        }
                        
                        Double reading = null;
                        if (i + 1 < parts.length) {
                            String readingStr = parts[i + 1].trim();
                            if (!readingStr.isEmpty()) {
                                try {
                                    reading = Double.parseDouble(readingStr);
                                } catch (NumberFormatException e) {
                                    // Игнорируем неверные показания
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
}
