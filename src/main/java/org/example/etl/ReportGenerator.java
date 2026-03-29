package org.example.etl;

import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Генератор отчётов по результатам ETL-обработки
 */
@Service
public class ReportGenerator {

    private final Charset charset;

    public ReportGenerator() {
        this(StandardCharsets.UTF_8);
    }

    public ReportGenerator(Charset charset) {
        this.charset = charset;
    }

    /**
     * Сгенерировать отчёт об ошибках
     */
    public void generateErrorReport(String filePath, EtlResult result,
                                    Map<String, List<String>> errorSamples) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), charset))) {

            writer.write("================================================================================\n");
            writer.write("ОТЧЕТ ПО ОШИБКАМ\n");
            writer.write("================================================================================\n\n");

            int totalErrors = result.getErrorCounts().values().stream().mapToInt(Integer::intValue).sum();
            int totalCorrected = result.getCorrectedErrorCounts().values().stream().mapToInt(Integer::intValue).sum();
            int totalUncorrected = totalErrors - totalCorrected;

            writer.write(String.format("Всего записей: %,d\n", result.getTotalCount()));
            writer.write(String.format("Всего ошибок: %,d\n", totalErrors));
            writer.write(String.format("Исправлено ошибок: %,d (%.2f%%)\n", totalCorrected, (double) totalCorrected / result.getTotalCount() * 100));
            writer.write(String.format("Не исправлено: %,d (%.2f%%)\n", totalUncorrected, (double) totalUncorrected / result.getTotalCount() * 100));
            writer.write(String.format("Типов ошибок: %,d\n\n", result.getErrorCounts().size()));
            writer.write("Примечание: Некоторые ошибки (пустая часть адреса) не могут быть исправлены\n");
            writer.write("автоматически без потери данных или требуют ручного вмешательства.\n\n");

            writer.write("--------------------------------------------------------------------------------\n");
            writer.write("ГРУППИРОВКА ПО ТИПАМ ОШИБОК:\n");
            writer.write("--------------------------------------------------------------------------------\n\n");

            // Сортируем ошибки по количеству (убывание)
            List<Map.Entry<String, Integer>> sortedErrors = new ArrayList<>(result.getErrorCounts().entrySet());
            sortedErrors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<String, Integer> entry : sortedErrors) {
                String errorType = entry.getKey();
                int count = entry.getValue();
                int corrected = result.getCorrectedErrorCounts().getOrDefault(errorType, 0);
                int uncorrected = count - corrected;
                double percent = (double) count / result.getTotalCount() * 100;
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
            throw new RuntimeException("Ошибка записи отчёта: " + e.getMessage(), e);
        }
    }

    /**
     * Отформатировать статистику для вывода в консоль
     */
    public String formatConsoleOutput(EtlResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("=== Результаты обработки ===\n");
        sb.append("Всего обработано записей: ").append(result.getTotalCount()).append("\n");
        sb.append(String.format("Успешно обработано: %d (%.2f%%)\n", result.getValidCount(), result.getValidPercent()));
        sb.append(String.format("С ошибками: %d (%.2f%%)\n", result.getInvalidCount(), result.getInvalidPercent()));
        sb.append(String.format("Исправлено записей: %d (%.2f%% от ошибок)\n", result.getCorrectedCount(), result.getCorrectedPercent()));
        sb.append(String.format("Не исправлено записей: %d (%.2f%% от ошибок)\n", result.getUncorrectedCount(), result.getUncorrectedPercent()));
        sb.append("\n");
        sb.append("=== Статистика базы данных ===\n");
        sb.append("Записей сохранено в базу данных: ").append(result.getDbSavedCount()).append("\n");
        sb.append("Ошибок при сохранении в БД: ").append(result.getDbErrorCount()).append("\n");
        sb.append("\n");
        sb.append("Баланс: ").append(result.getTotalCount())
            .append(" (всего) - ").append(result.getUncorrectedCount())
            .append(" (не исправлено) - ").append(result.getDbErrorCount())
            .append(" (ошибки БД) = ")
            .append(result.getTotalCount() - result.getUncorrectedCount() - result.getDbErrorCount())
            .append("\n");

        // Детальная статистика по ошибкам БД
        if (!result.getDbErrorCounts().isEmpty()) {
            sb.append("\n");
            sb.append("=== Детальная статистика ошибок БД ===\n");
            sb.append("Всего типов ошибок: ").append(result.getDbErrorCounts().size()).append("\n");
            sb.append("\n");

            List<Map.Entry<String, Integer>> sortedDbErrors = new ArrayList<>(result.getDbErrorCounts().entrySet());
            sortedDbErrors.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<String, Integer> entry : sortedDbErrors) {
                String errorType = entry.getKey();
                int count = entry.getValue();
                double percent = result.getDbErrorCount() > 0 ? (double) count / result.getDbErrorCount() * 100 : 0;

                sb.append(String.format("  %s: %d (%.2f%%)\n", errorType, count, percent));
            }
        }

        return sb.toString();
    }

    /**
     * Создать карту с примерами ошибок (до N на каждый тип)
     */
    public <T> Map<String, List<T>> createErrorSamples(Map<String, List<T>> samples, String errorType, T sample, int maxSamples) {
        samples.computeIfAbsent(errorType, k -> new ArrayList<>());
        if (samples.get(errorType).size() < maxSamples) {
            samples.get(errorType).add(sample);
        }
        return samples;
    }
}
