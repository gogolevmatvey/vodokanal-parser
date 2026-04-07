package org.example.etl;

import org.example.model.common.ParseResult;
import org.example.service.parsing.RecordParser;
import org.example.service.parsing.RecordCorrector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Основной сервис для выполнения ETL-обработки данных
 */
@Service
public class EtlService {

    private final RecordParser parser;
    private final RecordCorrector corrector;
    private final DatabaseImportService importService;
    private final ReportGenerator reportGenerator;
    private final EtlConfig config;
    private final Charset charset;

    @Autowired
    public EtlService(RecordParser parser,
                      RecordCorrector corrector,
                      DatabaseImportService importService,
                      ReportGenerator reportGenerator,
                      EtlConfig config) {
        this(parser, corrector, importService, reportGenerator, config, StandardCharsets.UTF_8);
    }

    // Второй конструктор без @Autowired для ручного создания
    public EtlService(RecordParser parser,
                      RecordCorrector corrector,
                      DatabaseImportService importService,
                      ReportGenerator reportGenerator,
                      EtlConfig config,
                      Charset charset) {
        this.parser = parser;
        this.corrector = corrector;
        this.importService = importService;
        this.reportGenerator = reportGenerator;
        this.config = config;
        this.charset = charset;
    }

    /**
     * Выполнить ETL-обработку
     */
    public EtlResult process() {
        long totalStartTime = System.nanoTime();

        int validCount = 0;
        int invalidCount = 0;
        int correctedCount = 0;
        int uncorrectedCount = 0;
        int totalCount = 0;
        int dbSavedCount = 0;
        int dbErrorCount = 0;

        long parseTimeNs = 0;
        long correctTimeNs = 0;
        long dbImportTimeNs = 0;
        long fileWriteTimeNs = 0;

        // Метрики памяти
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long peakHeapMemory = 0;
        long totalHeapMemory = 0;
        int memorySampleCount = 0;

        Map<String, Integer> errorCounts = new TreeMap<>();
        Map<String, Integer> correctedErrorCounts = new TreeMap<>();
        Map<String, Integer> dbErrorCounts = new TreeMap<>();
        Map<String, List<String>> errorSamples = new TreeMap<>();
        Map<String, List<String>> dbErrorSamples = new TreeMap<>();

        List<String> dbBatch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(config.getInputFilePath()),
                            charset
                    )
                );
                BufferedWriter validWriter = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(config.getValidOutputPath()), charset)
                );
                BufferedWriter invalidWriter = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(config.getInvalidOutputPath()), charset)
                );
                BufferedWriter correctedWriter = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(config.getCorrectedOutputPath()), charset)
                );
                BufferedWriter uncorrectedWriter = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(config.getUncorrectedOutputPath()), charset)
                )) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                totalCount++;

                // Замер времени парсинга
                long parseStart = System.nanoTime();
                ParseResult result = parser.parse(line);
                parseTimeNs += System.nanoTime() - parseStart;

                if (result.isValid()) {
                    validCount++;

                    // Замер времени записи в файл
                    long fileWriteStart = System.nanoTime();
                    validWriter.write(line);
                    validWriter.newLine();
                    fileWriteTimeNs += System.nanoTime() - fileWriteStart;

                    dbBatch.add(line);
                    if (dbBatch.size() >= config.getBatchSize()) {
                        long dbStart = System.nanoTime();
                        var batchResult = importService.saveBatch(dbBatch, 5);
                        dbImportTimeNs += System.nanoTime() - dbStart;
                        dbSavedCount += batchResult.getSavedCount();
                        dbErrorCount += batchResult.getErrorCount();
                        mergeErrorStats(dbErrorCounts, dbErrorSamples, batchResult);
                        dbBatch.clear();
                    }
                } else {
                    invalidCount++;

                    // Замер времени записи в файл
                    long fileWriteStart = System.nanoTime();
                    invalidWriter.write(line + " | Ошибка: " + result.getErrorMessage());
                    invalidWriter.newLine();
                    fileWriteTimeNs += System.nanoTime() - fileWriteStart;

                    // Замер времени коррекции
                    long correctStart = System.nanoTime();
                    String correctedLine = corrector.correct(line, result.getErrorMessage());
                    correctTimeNs += System.nanoTime() - correctStart;

                    String errorType = extractErrorType(result.getErrorMessage());
                    boolean wasCorrected = isRecordCorrected(correctedLine, errorType);

                    if (wasCorrected) {
                        // Замер времени записи в файл
                        long fileWriteStart2 = System.nanoTime();
                        correctedWriter.write(correctedLine);
                        correctedWriter.newLine();
                        fileWriteTimeNs += System.nanoTime() - fileWriteStart2;

                        correctedCount++;
                        correctedErrorCounts.put(errorType, correctedErrorCounts.getOrDefault(errorType, 0) + 1);

                        dbBatch.add(correctedLine);
                        if (dbBatch.size() >= config.getBatchSize()) {
                            long dbStart = System.nanoTime();
                            var batchResult = importService.saveBatch(dbBatch, 5);
                            dbImportTimeNs += System.nanoTime() - dbStart;
                            dbSavedCount += batchResult.getSavedCount();
                            dbErrorCount += batchResult.getErrorCount();
                            mergeErrorStats(dbErrorCounts, dbErrorSamples, batchResult);
                            dbBatch.clear();
                        }
                    } else {
                        // Замер времени записи в файл
                        long fileWriteStart2 = System.nanoTime();
                        uncorrectedWriter.write(line + " | Ошибка: " + result.getErrorMessage());
                        uncorrectedWriter.newLine();
                        fileWriteTimeNs += System.nanoTime() - fileWriteStart2;

                        uncorrectedCount++;
                    }

                    errorCounts.put(errorType, errorCounts.getOrDefault(errorType, 0) + 1);
                    reportGenerator.createErrorSamples(errorSamples, errorType,
                            line.substring(0, Math.min(150, line.length())), 5);
                }

                // Собираем метрики памяти каждые 1000 записей
                if (totalCount % 1000 == 0) {
                    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
                    long usedHeap = heapUsage.getUsed();
                    peakHeapMemory = Math.max(peakHeapMemory, usedHeap);
                    totalHeapMemory += usedHeap;
                    memorySampleCount++;
                }

                if (totalCount % config.getProgressInterval() == 0) {
                    System.out.printf("Обработано строк: %d, Валидных: %d, Ошибок: %d, Исправлено: %d, Не исправлено: %d, В БД: %d, Ошибок БД: %d%n",
                            totalCount, validCount, invalidCount, correctedCount, uncorrectedCount, dbSavedCount, dbErrorCount);
                }
            }

            // Сохраняем остаток записей в БД
            if (!dbBatch.isEmpty()) {
                long dbStart = System.nanoTime();
                var batchResult = importService.saveBatch(dbBatch, 5);
                dbImportTimeNs += System.nanoTime() - dbStart;
                dbSavedCount += batchResult.getSavedCount();
                dbErrorCount += batchResult.getErrorCount();
                mergeErrorStats(dbErrorCounts, dbErrorSamples, batchResult);
            }

            // Финальный замер памяти
            MemoryUsage finalHeapUsage = memoryBean.getHeapMemoryUsage();
            peakHeapMemory = Math.max(peakHeapMemory, finalHeapUsage.getUsed());
            totalHeapMemory += finalHeapUsage.getUsed();
            memorySampleCount++;

        } catch (IOException e) {
            throw new RuntimeException("Ошибка обработки файла: " + e.getMessage(), e);
        }

        long totalEndTime = System.nanoTime();
        long totalTimeMs = (totalEndTime - totalStartTime) / 1_000_000;

        long parseTimeMs = parseTimeNs / 1_000_000;
        long correctTimeMs = correctTimeNs / 1_000_000;
        long dbImportTimeMs = dbImportTimeNs / 1_000_000;
        long fileWriteTimeMs = fileWriteTimeNs / 1_000_000;

        long avgHeapMemory = memorySampleCount > 0 ? totalHeapMemory / memorySampleCount : 0;

        // Получаем метрики БД из importService
        long totalDbQueryTimeMs = importService.getTotalQueryTimeMs();
        int totalDbQueries = importService.getTotalQueries();

        // Получаем метрики кэша из importService
        long cacheHits = importService.getCacheHits();
        long cacheMisses = importService.getCacheMisses();
        int localityCacheSize = importService.getLocalityCacheSize();
        int streetCacheSize = importService.getStreetCacheSize();
        int houseCacheSize = importService.getHouseCacheSize();
        int apartmentCacheSize = importService.getApartmentCacheSize();

        // Генерируем отчёт
        reportGenerator.generateErrorReport(config.getErrorReportPath(),
                EtlResult.builder()
                        .totalCount(totalCount)
                        .validCount(validCount)
                        .invalidCount(invalidCount)
                        .correctedCount(correctedCount)
                        .uncorrectedCount(uncorrectedCount)
                        .dbSavedCount(dbSavedCount)
                        .dbErrorCount(dbErrorCount)
                        .errorCounts(errorCounts)
                        .correctedErrorCounts(correctedErrorCounts)
                        .dbErrorCounts(dbErrorCounts)
                        .totalTimeMs(totalTimeMs)
                        .parseTimeMs(parseTimeMs)
                        .correctTimeMs(correctTimeMs)
                        .dbImportTimeMs(dbImportTimeMs)
                        .fileWriteTimeMs(fileWriteTimeMs)
                        .peakHeapMemoryBytes(peakHeapMemory)
                        .avgHeapMemoryBytes(avgHeapMemory)
                        .totalDbQueryTimeMs(totalDbQueryTimeMs)
                        .totalDbQueries(totalDbQueries)
                        .cacheHits(cacheHits)
                        .cacheMisses(cacheMisses)
                        .localityCacheSize(localityCacheSize)
                        .streetCacheSize(streetCacheSize)
                        .houseCacheSize(houseCacheSize)
                        .apartmentCacheSize(apartmentCacheSize)
                        .build(),
                errorSamples);

        return EtlResult.builder()
                .totalCount(totalCount)
                .validCount(validCount)
                .invalidCount(invalidCount)
                .correctedCount(correctedCount)
                .uncorrectedCount(uncorrectedCount)
                .dbSavedCount(dbSavedCount)
                .dbErrorCount(dbErrorCount)
                .errorCounts(errorCounts)
                .correctedErrorCounts(correctedErrorCounts)
                .dbErrorCounts(dbErrorCounts)
                .totalTimeMs(totalTimeMs)
                .parseTimeMs(parseTimeMs)
                .correctTimeMs(correctTimeMs)
                .dbImportTimeMs(dbImportTimeMs)
                .fileWriteTimeMs(fileWriteTimeMs)
                .peakHeapMemoryBytes(peakHeapMemory)
                .avgHeapMemoryBytes(avgHeapMemory)
                .totalDbQueryTimeMs(totalDbQueryTimeMs)
                .totalDbQueries(totalDbQueries)
                .cacheHits(cacheHits)
                .cacheMisses(cacheMisses)
                .localityCacheSize(localityCacheSize)
                .streetCacheSize(streetCacheSize)
                .houseCacheSize(houseCacheSize)
                .apartmentCacheSize(apartmentCacheSize)
                .build();
    }

    /**
     * Объединить статистику ошибок из пакетной вставки
     */
    private void mergeErrorStats(Map<String, Integer> errorCounts,
                                  Map<String, List<String>> errorSamples,
                                  DatabaseImportService.BatchResult batchResult) {
        for (Map.Entry<String, Integer> entry : batchResult.getErrorCounts().entrySet()) {
            errorCounts.put(entry.getKey(), errorCounts.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }
        for (Map.Entry<String, List<String>> entry : batchResult.getErrorSamples().entrySet()) {
            errorSamples.putIfAbsent(entry.getKey(), new ArrayList<>());
            for (String sample : entry.getValue()) {
                if (errorSamples.get(entry.getKey()).size() < 5) {
                    errorSamples.get(entry.getKey()).add(sample);
                }
            }
        }
    }

    /**
     * Извлечь тип ошибки из сообщения
     */
    private String extractErrorType(String errorMessage) {
        if (errorMessage.contains(":")) {
            return errorMessage.substring(0, errorMessage.indexOf(":")).trim();
        }
        return errorMessage.trim();
    }

    /**
     * Проверяет, была ли ошибка исправлена в записи
     */
    private boolean isRecordCorrected(String correctedLine, String errorType) {
        switch (errorType) {
            case "Пустое ФИО плательщика":
                String[] nameParts = correctedLine.split(";");
                if (nameParts.length > 1) {
                    return !nameParts[1].trim().isEmpty();
                }
                return false;

            case "Неполный адрес":
            case "Неполный адрес (меньше 3 запятых)":
                String[] addrParts = correctedLine.split(";");
                if (addrParts.length > 2) {
                    int commaCount = countChar(addrParts[2], ',');
                    return commaCount >= 3 || addrParts[2].contains("не указан");
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
                return false;

            case "Подозрительный адрес":
            case "Подозрительный адрес (больше 5 запятых)":
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
     * Подсчитывает количество вхождений символа в строку
     */
    private int countChar(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
}
