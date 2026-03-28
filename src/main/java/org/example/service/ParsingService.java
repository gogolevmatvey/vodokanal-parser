package org.example.service;

import org.example.parser.model.ParseResult;
import org.example.parser.model.ParsedRecord;
import org.example.repository.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для парсинга и импорта файлов
 */
public class ParsingService {
    private final RecordParser parser;
    private final RecordCorrector corrector;
    private final AccountRepository accountRepo;
    private final BillingPeriodRepository billingRepo;
    private final MeterChargeRepository meterRepo;
    
    public ParsingService(RecordParser parser, RecordCorrector corrector,
                          AccountRepository accountRepo, BillingPeriodRepository billingRepo,
                          MeterChargeRepository meterRepo) {
        this.parser = parser;
        this.corrector = corrector;
        this.accountRepo = accountRepo;
        this.billingRepo = billingRepo;
        this.meterRepo = meterRepo;
    }
    
    /**
     * Результат парсинга
     */
    public static class ParsingResult {
        private final int validCount;
        private final int invalidCount;
        private final int correctedCount;
        private final int dbErrorCount;
        
        public ParsingResult(int validCount, int invalidCount, int correctedCount, int dbErrorCount) {
            this.validCount = validCount;
            this.invalidCount = invalidCount;
            this.correctedCount = correctedCount;
            this.dbErrorCount = dbErrorCount;
        }
        
        public int getValidCount() { return validCount; }
        public int getInvalidCount() { return invalidCount; }
        public int getCorrectedCount() { return correctedCount; }
        public int getDbErrorCount() { return dbErrorCount; }
        
        @Override
        public String toString() {
            return String.format("ParsingResult{valid=%d, invalid=%d, corrected=%d, dbErrors=%d}",
                validCount, invalidCount, correctedCount, dbErrorCount);
        }
    }
    
    /**
     * Парсинг и импорт файла
     */
    public ParsingResult parseAndImport(File file) throws IOException {
        AtomicInteger validCount = new AtomicInteger();
        AtomicInteger invalidCount = new AtomicInteger();
        AtomicInteger correctedCount = new AtomicInteger();
        AtomicInteger dbErrorCount = new AtomicInteger();
        
        Files.lines(file.toPath())
            .forEach(line -> {
                if (line.trim().isEmpty()) {
                    return;
                }
                
                ParseResult parseResult = parser.parse(line);
                if (parseResult.isValid()) {
                    validCount.incrementAndGet();
                    // Сохранение в БД
                    saveToDatabase(parseResult.getRecordData());
                } else {
                    invalidCount.incrementAndGet();
                    String corrected = corrector.correct(line, parseResult.getErrorMessage());
                    if (corrected != null) {
                        correctedCount.incrementAndGet();
                    }
                }
            });
        
        return new ParsingResult(
            validCount.get(),
            invalidCount.get(),
            correctedCount.get(),
            dbErrorCount.get()
        );
    }
    
    /**
     * Сохранение распаршенной записи в БД
     */
    private void saveToDatabase(ParsedRecord record) {
        // TODO: Реализовать сохранение
    }
}
