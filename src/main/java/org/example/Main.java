package org.example;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import static com.mongodb.client.model.Filters.*;

public class Main {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017/";
    private static final String DATABASE_NAME = "Vodokonal-parser-db";
    private static final String COLLECTION_NAME = "Vodokonal-parser-collection";

    private static final int PROGRESS_INTERVAL = 1000;

    public static void main(String[] args) {
        try (MongoClient mongoClient = MongoClients.create(CONNECTION_STRING)) {
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);

            // Очистка коллекции перед новой загрузкой
            System.out.println("Очищаем коллекцию...");
            collection.drop();
            System.out.println("Коллекция очищена");

            String filePath = "Testovye_dannye (1).txt"; // Путь к вашему файлу

            int validCount = 0;
            int invalidCount = 0;
            List<String> invalidRecords = new ArrayList<>();

            int totalCount = 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(filePath),
                            Charset.forName("UTF-8") // Changed from windows-1251 to UTF-8
                    )
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    totalCount++;

                    ParseResult result = parseRecord(line);
                    Document doc = new Document()
                            .append("originalLine", line)
                            .append("isValid", result.isValid())
                            .append("validationError", result.errorMessage());

                    if (result.isValid()) {
                        // Добавляем поля данных только если запись валидна
                        doc.append("accountNumber", result.recordData().accountNumber())
                                .append("payerName", result.recordData().payerName())
                                .append("address", result.recordData().address())
                                .append("billingPeriod", result.recordData().billingPeriod())
                                .append("charges", result.recordData().charges());
                        validCount++;
                    } else {
                        invalidCount++;
                        invalidRecords.add(result.errorMessage() + ": " + line);
                    }

                    collection.insertOne(doc);

                    if (totalCount % PROGRESS_INTERVAL == 0) {
                        System.out.printf("Обработано строк: %d, Валидных: %d, Ошибок: %d%n",
                                totalCount, validCount, invalidCount);
                    }
                }
            }

            // Вывод статистики
            long totalRecords = validCount + invalidCount;
            double validPercent = totalRecords > 0 ? (double) validCount / totalRecords * 100 : 0;
            double invalidPercent = totalRecords > 0 ? (double) invalidCount / totalRecords * 100 : 0;

            System.out.println("=== Результаты обработки ===");
            System.out.println("Всего обработано записей: " + totalRecords);
            System.out.println("Успешно обработано: " + validCount + " (" + String.format("%.2f", validPercent) + "%)");
            System.out.println("С ошибками: " + invalidCount + " (" + String.format("%.2f", invalidPercent) + "%)");

            if (!invalidRecords.isEmpty()) {
                System.out.println("\nПримеры невалидных записей:");
                for (int i = 0; i < Math.min(5, invalidRecords.size()); i++) {
                    System.out.println("- " + invalidRecords.get(i));
                }
            }

        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка подключения к MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
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
            Document address,
            String billingPeriod,
            List<Document> charges
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

            // 2. ФИО плательщика
            String payerName = parts[1].trim();
            if (payerName.isEmpty()) {
                return new ParseResult(false, "Пустое ФИО плательщика", null);
            }

            // 3. Адрес (разделенный запятыми)
            String addressStr = parts[2].trim();
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

            Document addressDoc = new Document()
                    .append("locality", locality)
                    .append("street", street)
                    .append("house", house)
                    .append("apartments", apartments);

            // 4. Период начисления
            String billingPeriod = parts[3].trim();
            if (billingPeriod.isEmpty()) {
                return new ParseResult(false, "Пустой период начисления", null);
            }

            // 5. Суммы начисления и приборы учета
            List<Document> charges = new ArrayList<>();

            // Если в строке 5 частей - это простой случай (только сумма)
            if (parts.length == 5) {
                try {
                    double amount = Double.parseDouble(parts[4].trim());
                    charges.add(new Document()
                            .append("amount", amount)
                            .append("meter", null));
                } catch (NumberFormatException e) {
                    return new ParseResult(false, "Неверный формат суммы начисления: " + parts[4], null);
                }
            }
            // Иначе обрабатываем пары полей (сумма, прибор учета)
            else {
                for (int i = 4; i < parts.length; i += 2) {
                    try {
                        double amount = Double.parseDouble(parts[i].trim());

                        // Проверяем наличие следующего поля (прибора учета)
                        String meter = null;
                        if (i + 1 < parts.length) {
                            meter = parts[i + 1].trim();
                            if (meter.isEmpty()) meter = null;
                        }

                        charges.add(new Document()
                                .append("amount", amount)
                                .append("meter", meter));
                    } catch (NumberFormatException e) {
                        return new ParseResult(false, "Неверный формат суммы начисления: " + parts[i], null);
                    }
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
            ParseResult result = new ParseResult(false, "Ошибка парсинга строки: " + e.getMessage(), null);
            return result;
        }
    }
}
