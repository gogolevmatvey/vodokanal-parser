package org.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Анализатор файла valid_records.txt для поиска дополнительных ошибок.
 */
public class AnalyzeValidRecords {

    private static final int PROGRESS_INTERVAL = 50000;
    private static final int MAX_SAMPLES_PER_ERROR = 5;

    // Счетчики ошибок
    private static final Map<String, Integer> errorCounts = new TreeMap<>();
    // Примеры ошибок
    private static final Map<String, List<ErrorSample>> errorSamples = new HashMap<>();

    private record ErrorSample(int lineNum, String detail, String fullLine) {}

    public static void main(String[] args) {
        String filePath = "valid_records.txt";
        String outputFile = "analysis_report.txt";
        
        String userDir = System.getProperty("user.dir");
        System.out.println("Текущая директория: " + userDir);
        System.out.println("Полный путь к файлу: " + userDir + "\\" + filePath);

        int totalLines = 0;
        int linesWithErrors = 0;

        System.out.println("Начало анализа файла: " + filePath);

        try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(filePath),
                            Charset.forName("UTF-8")
                    )
                )) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                totalLines++;

                List<String> errors = checkRecord(lineNum, line);

                if (!errors.isEmpty()) {
                    linesWithErrors++;
                    for (String error : errors) {
                        // Разделяем тип ошибки и деталь
                        String errorType = error.split("\\|")[0].trim();
                        String errorDetail = error.split("\\|")[1].trim();

                        errorCounts.put(errorType, errorCounts.getOrDefault(errorType, 0) + 1);

                        List<ErrorSample> samples = errorSamples.computeIfAbsent(errorType, k -> new ArrayList<>());
                        if (samples.size() < MAX_SAMPLES_PER_ERROR) {
                            samples.add(new ErrorSample(lineNum, errorDetail, line.trim()));
                        }
                    }
                }

                if (lineNum % PROGRESS_INTERVAL == 0) {
                    System.out.printf("Обработано строк: %d, Найдено строк с ошибками: %d%n",
                            lineNum, linesWithErrors);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.printf("Анализ завершен. Всего строк: %d, с ошибками: %d%n", totalLines, linesWithErrors);
        System.out.println("Запись отчета...");

        writeReport(outputFile, totalLines, linesWithErrors);

        System.out.println("Отчет записан в: " + outputFile);
    }

    /**
     * Проверяет одну запись на наличие ошибок.
     * Возвращает список ошибок в формате "Тип ошибки | Детали"
     */
    private static List<String> checkRecord(int lineNum, String line) {
        List<String> errors = new ArrayList<>();
        line = line.trim();

        if (line.isEmpty()) {
            errors.add("Пустая строка | ");
            return errors;
        }

        String[] parts = line.split(";");

        // Проверка количества полей
        if (parts.length < 5) {
            errors.add("Недостаточно полей | " + parts.length + " полей вместо минимум 5");
            return errors;
        }

        String accountNumber = parts[0].trim();
        String payerName = parts[1].trim();
        String address = parts[2].trim();
        String billingPeriod = parts[3].trim();

        // 1. Проверка номера лицевого счета
        if (accountNumber.isEmpty()) {
            errors.add("Пустой номер лицевого счета | ");
        } else if (!accountNumber.matches("^\\d+$")) {
            errors.add("Неверный формат номера счета | " + accountNumber);
        }

        // 2. Проверка ФИО плательщика
        if (payerName.isEmpty() || payerName.equals(".*") || payerName.matches("^\\s*$")) {
            errors.add("Пустое ФИО плательщика | ");
        } else if (payerName.matches("^[.\\*\\s]+$")) {
            errors.add("Пустое ФИО плательщика (только спецсимволы) | " + payerName);
        } else if (payerName.matches("^[\\d\\s]+$")) {
            errors.add("ФИО состоит только из цифр | " + payerName);
        }

        // 3. Проверка адреса
        int commaCount = countChar(address, ',');
        if (commaCount < 3) {
            errors.add("Неполный адрес (меньше 3 запятых) | " + commaCount + " запятых");
        } else if (commaCount > 5) {
            errors.add("Подозрительный адрес (больше 5 запятых) | " + commaCount + " запятых");
        }

        // Проверка на пустые части адреса
        String[] addressParts = address.split(",");
        for (int i = 0; i < Math.min(3, addressParts.length); i++) {
            if (addressParts[i].trim().isEmpty()) {
                errors.add("Пустая часть адреса | Часть " + (i + 1) + " пуста");
            }
        }

        // 4. Проверка периода начисления
        if (billingPeriod.isEmpty()) {
            errors.add("Пустой период начисления | ");
        } else if (!billingPeriod.matches("^\\d{2,4}$")) {
            // Проверяем на текстовый формат месяца
            if (isTextualMonth(billingPeriod)) {
                errors.add("Период в текстовом формате (месяц) | " + billingPeriod);
            } else if (!billingPeriod.matches("^\\d+$")) {
                errors.add("Неверный формат периода | " + billingPeriod);
            }
        }

        // 5. Проверка сумм и приборов учета
        for (int i = 4; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                continue;
            }

            // Проверяем, является ли часть числом (сумма или показания)
            // Поддерживаем форматы: 123, 123.45, 185.0000
            if (part.matches("^\\d+\\.?\\d*$") || part.matches("^\\d*\\.\\d+$")) {
                continue; // Это число, всё ок
            }

            // Проверяем, не является ли это текстовым месяцем
            if (isTextualMonth(part)) {
                errors.add("Текстовый месяц в поле данных | " + part + " (поле " + (i + 1) + ")");
                continue;
            }

            // Проверяем, не является ли это замаскированным ФИО
            if (part.equals(".*")) {
                continue;
            }

            // Прибор учета обычно содержит буквы (русские и латинские), цифры, дефисы, слеши, запятые, плюс, обратный слэш, @, _, [, ?
            if (part.matches("^[A-Za-zА-Яа-яЁё0-9№@_\\[?\\\\\\s\\-/,+()]+.$") || part.matches("^[A-Za-zА-Яа-яЁё0-9№@_\\[?\\\\\\s\\-/,+()]+$")) {
                continue; // Это прибор учета, всё ок
            }

            // Подозрительные значения (проверка на допустимые символы)
            if (!part.matches("^[A-Za-zА-Яа-яЁё0-9№@_\\[?\\\\\\s\\-/.:(),+]+$")) {
                errors.add("Подозрительное значение | " + truncate(part, 50) + " (поле " + (i + 1) + ")");
            }
        }

        return errors;
    }

    private static int countChar(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    private static boolean isTextualMonth(String s) {
        Pattern monthPattern = Pattern.compile("^(янв|фев|мар|апр|май|июн|июл|авг|сен|окт|ноя|дек)\\.?\\d{2}$", Pattern.CASE_INSENSITIVE);
        Pattern monthPattern2 = Pattern.compile("^\\d{2}\\.(янв|фев|мар|апр|май|июн|июл|авг|сен|окт|ноя|дек)$", Pattern.CASE_INSENSITIVE);
        Matcher m1 = monthPattern.matcher(s);
        Matcher m2 = monthPattern2.matcher(s);
        return m1.matches() || m2.matches();
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "...";
    }

    private static void writeReport(String outputFile, int totalLines, int linesWithErrors) {
        try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outputFile), Charset.forName("UTF-8"))
                )) {
            writer.write("================================================================================\n");
            writer.write("ОТЧЕТ ПО АНАЛИЗУ valid_records.txt\n");
            writer.write("================================================================================\n\n");

            writer.write(String.format("Всего строк: %,d\n", totalLines));
            writer.write(String.format("Строк с ошибками: %,d\n", linesWithErrors));
            writer.write(String.format("Строк без ошибок: %,d\n", totalLines - linesWithErrors));
            writer.write(String.format("Процент ошибок: %.2f%%\n\n", (double) linesWithErrors / totalLines * 100));

            writer.write("--------------------------------------------------------------------------------\n");
            writer.write("ТИПЫ ОБНАРУЖЕННЫХ ОШИБОК:\n");
            writer.write("--------------------------------------------------------------------------------\n\n");

            // Сортируем ошибки по количеству (убывание)
            List<Map.Entry<String, Integer>> sortedErrors = new ArrayList<>(errorCounts.entrySet());
            sortedErrors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<String, Integer> entry : sortedErrors) {
                String errorType = entry.getKey();
                int count = entry.getValue();

                writer.write(String.format("\n### %s: %,d случаев\n", errorType, count));
                writer.write("   Примеры:\n");

                List<ErrorSample> samples = errorSamples.get(errorType);
                if (samples != null) {
                    for (ErrorSample sample : samples) {
                        writer.write(String.format("   - Строка %d: %s\n", sample.lineNum, sample.detail));
                        writer.write(String.format("     %s\n", truncate(sample.fullLine, 150)));
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
}
