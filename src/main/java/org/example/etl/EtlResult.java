package org.example.etl;

import java.util.Map;

/**
 * Результат выполнения ETL-процесса
 */
public class EtlResult {

    private final int totalCount;
    private final int validCount;
    private final int invalidCount;
    private final int correctedCount;
    private final int uncorrectedCount;
    private final int dbSavedCount;
    private final int dbErrorCount;
    private final Map<String, Integer> errorCounts;
    private final Map<String, Integer> correctedErrorCounts;
    private final Map<String, Integer> dbErrorCounts;

    // === Метрики времени (мс) ===
    private final long totalTimeMs;
    private final long parseTimeMs;
    private final long correctTimeMs;
    private final long dbImportTimeMs;
    private final long fileWriteTimeMs;

    // === Метрики памяти (байты) ===
    private final long peakHeapMemoryBytes;
    private final long avgHeapMemoryBytes;

    // === Метрики БД ===
    private final long totalDbQueryTimeMs;
    private final int totalDbQueries;

    // === Метрики кэша ===
    private final long cacheHits;
    private final long cacheMisses;
    private final int localityCacheSize;
    private final int streetCacheSize;
    private final int houseCacheSize;
    private final int apartmentCacheSize;

    private EtlResult(Builder builder) {
        this.totalCount = builder.totalCount;
        this.validCount = builder.validCount;
        this.invalidCount = builder.invalidCount;
        this.correctedCount = builder.correctedCount;
        this.uncorrectedCount = builder.uncorrectedCount;
        this.dbSavedCount = builder.dbSavedCount;
        this.dbErrorCount = builder.dbErrorCount;
        this.errorCounts = builder.errorCounts;
        this.correctedErrorCounts = builder.correctedErrorCounts;
        this.dbErrorCounts = builder.dbErrorCounts;

        this.totalTimeMs = builder.totalTimeMs;
        this.parseTimeMs = builder.parseTimeMs;
        this.correctTimeMs = builder.correctTimeMs;
        this.dbImportTimeMs = builder.dbImportTimeMs;
        this.fileWriteTimeMs = builder.fileWriteTimeMs;

        this.peakHeapMemoryBytes = builder.peakHeapMemoryBytes;
        this.avgHeapMemoryBytes = builder.avgHeapMemoryBytes;

        this.totalDbQueryTimeMs = builder.totalDbQueryTimeMs;
        this.totalDbQueries = builder.totalDbQueries;

        this.cacheHits = builder.cacheHits;
        this.cacheMisses = builder.cacheMisses;
        this.localityCacheSize = builder.localityCacheSize;
        this.streetCacheSize = builder.streetCacheSize;
        this.houseCacheSize = builder.houseCacheSize;
        this.apartmentCacheSize = builder.apartmentCacheSize;
    }

    public int getTotalCount() { return totalCount; }
    public int getValidCount() { return validCount; }
    public int getInvalidCount() { return invalidCount; }
    public int getCorrectedCount() { return correctedCount; }
    public int getUncorrectedCount() { return uncorrectedCount; }
    public int getDbSavedCount() { return dbSavedCount; }
    public int getDbErrorCount() { return dbErrorCount; }
    public Map<String, Integer> getErrorCounts() { return errorCounts; }
    public Map<String, Integer> getCorrectedErrorCounts() { return correctedErrorCounts; }
    public Map<String, Integer> getDbErrorCounts() { return dbErrorCounts; }

    // === Геттеры для метрик времени ===
    public long getTotalTimeMs() { return totalTimeMs; }
    public long getParseTimeMs() { return parseTimeMs; }
    public long getCorrectTimeMs() { return correctTimeMs; }
    public long getDbImportTimeMs() { return dbImportTimeMs; }
    public long getFileWriteTimeMs() { return fileWriteTimeMs; }

    // === Геттеры для метрик памяти ===
    public long getPeakHeapMemoryBytes() { return peakHeapMemoryBytes; }
    public long getAvgHeapMemoryBytes() { return avgHeapMemoryBytes; }

    // === Геттеры для метрик БД ===
    public long getTotalDbQueryTimeMs() { return totalDbQueryTimeMs; }
    public int getTotalDbQueries() { return totalDbQueries; }

    // === Геттеры для метрик кэша ===
    public long getCacheHits() { return cacheHits; }
    public long getCacheMisses() { return cacheMisses; }
    public long getCacheTotal() { return cacheHits + cacheMisses; }
    public int getLocalityCacheSize() { return localityCacheSize; }
    public int getStreetCacheSize() { return streetCacheSize; }
    public int getHouseCacheSize() { return houseCacheSize; }
    public int getApartmentCacheSize() { return apartmentCacheSize; }

    /**
     * Процент попаданий в кэш
     */
    public double getCacheHitRate() {
        long total = getCacheTotal();
        return total > 0 ? (double) cacheHits / total * 100 : 0;
    }

    /**
     * Сколько запросов к БД сэкономлено благодаря кэшу
     */
    public long getSavedDbQueries() {
        return cacheHits;
    }

    // === Вычисляемые метрики ===

    /**
     * Средняя скорость обработки (записей в секунду)
     */
    public double getRecordsPerSecond() {
        return totalTimeMs > 0 ? (double) totalCount / (totalTimeMs / 1000.0) : 0;
    }

    /**
     * Средняя скорость парсинга (записей в секунду)
     */
    public double getParseRecordsPerSecond() {
        return parseTimeMs > 0 ? (double) totalCount / (parseTimeMs / 1000.0) : 0;
    }

    /**
     * Средняя скорость импорта в БД (записей в секунду)
     */
    public double getDbRecordsPerSecond() {
        return dbImportTimeMs > 0 ? (double) dbSavedCount / (dbImportTimeMs / 1000.0) : 0;
    }

    /**
     * Среднее время запроса к БД (мс)
     */
    public double getAvgDbQueryTimeMs() {
        return totalDbQueries > 0 ? (double) totalDbQueryTimeMs / totalDbQueries : 0;
    }

    /**
     * Среднее время коррекции одной записи (мс)
     */
    public double getAvgCorrectTimeMs() {
        return correctedCount > 0 ? (double) correctTimeMs / correctedCount : 0;
    }

    /**
     * Пиковое использование JVM Heap (МБ)
     */
    public double getPeakHeapMemoryMb() {
        return peakHeapMemoryBytes / (1024.0 * 1024.0);
    }

    /**
     * Среднее использование JVM Heap (МБ)
     */
    public double getAvgHeapMemoryMb() {
        return avgHeapMemoryBytes / (1024.0 * 1024.0);
    }

    /**
     * Процент валидных записей
     */
    public double getValidPercent() {
        return totalCount > 0 ? (double) validCount / totalCount * 100 : 0;
    }

    /**
     * Процент ошибочных записей
     */
    public double getInvalidPercent() {
        return totalCount > 0 ? (double) invalidCount / totalCount * 100 : 0;
    }

    /**
     * Процент исправленных записей (от ошибочных)
     */
    public double getCorrectedPercent() {
        return invalidCount > 0 ? (double) correctedCount / invalidCount * 100 : 0;
    }

    /**
     * Процент неисправленных записей (от ошибочных)
     */
    public double getUncorrectedPercent() {
        return invalidCount > 0 ? (double) uncorrectedCount / invalidCount * 100 : 0;
    }

    @Override
    public String toString() {
        return String.format(
            "EtlResult{total=%d, valid=%d, invalid=%d, corrected=%d, uncorrected=%d, dbSaved=%d, dbErrors=%d}",
            totalCount, validCount, invalidCount, correctedCount, uncorrectedCount, dbSavedCount, dbErrorCount
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int totalCount;
        private int validCount;
        private int invalidCount;
        private int correctedCount;
        private int uncorrectedCount;
        private int dbSavedCount;
        private int dbErrorCount;
        private Map<String, Integer> errorCounts = Map.of();
        private Map<String, Integer> correctedErrorCounts = Map.of();
        private Map<String, Integer> dbErrorCounts = Map.of();

        // Метрики времени
        private long totalTimeMs;
        private long parseTimeMs;
        private long correctTimeMs;
        private long dbImportTimeMs;
        private long fileWriteTimeMs;

        // Метрики памяти
        private long peakHeapMemoryBytes;
        private long avgHeapMemoryBytes;

        // Метрики БД
        private long totalDbQueryTimeMs;
        private int totalDbQueries;

        // Метрики кэша
        private long cacheHits;
        private long cacheMisses;
        private int localityCacheSize;
        private int streetCacheSize;
        private int houseCacheSize;
        private int apartmentCacheSize;

        public Builder totalCount(int count) { this.totalCount = count; return this; }
        public Builder validCount(int count) { this.validCount = count; return this; }
        public Builder invalidCount(int count) { this.invalidCount = count; return this; }
        public Builder correctedCount(int count) { this.correctedCount = count; return this; }
        public Builder uncorrectedCount(int count) { this.uncorrectedCount = count; return this; }
        public Builder dbSavedCount(int count) { this.dbSavedCount = count; return this; }
        public Builder dbErrorCount(int count) { this.dbErrorCount = count; return this; }
        public Builder errorCounts(Map<String, Integer> counts) { this.errorCounts = counts; return this; }
        public Builder correctedErrorCounts(Map<String, Integer> counts) { this.correctedErrorCounts = counts; return this; }
        public Builder dbErrorCounts(Map<String, Integer> counts) { this.dbErrorCounts = counts; return this; }

        // Сеттеры для метрик времени
        public Builder totalTimeMs(long timeMs) { this.totalTimeMs = timeMs; return this; }
        public Builder parseTimeMs(long timeMs) { this.parseTimeMs = timeMs; return this; }
        public Builder correctTimeMs(long timeMs) { this.correctTimeMs = timeMs; return this; }
        public Builder dbImportTimeMs(long timeMs) { this.dbImportTimeMs = timeMs; return this; }
        public Builder fileWriteTimeMs(long timeMs) { this.fileWriteTimeMs = timeMs; return this; }

        // Сеттеры для метрик памяти
        public Builder peakHeapMemoryBytes(long bytes) { this.peakHeapMemoryBytes = bytes; return this; }
        public Builder avgHeapMemoryBytes(long bytes) { this.avgHeapMemoryBytes = bytes; return this; }

        // Сеттеры для метрик БД
        public Builder totalDbQueryTimeMs(long timeMs) { this.totalDbQueryTimeMs = timeMs; return this; }
        public Builder totalDbQueries(int count) { this.totalDbQueries = count; return this; }

        // Сеттеры для метрик кэша
        public Builder cacheHits(long hits) { this.cacheHits = hits; return this; }
        public Builder cacheMisses(long misses) { this.cacheMisses = misses; return this; }
        public Builder localityCacheSize(int size) { this.localityCacheSize = size; return this; }
        public Builder streetCacheSize(int size) { this.streetCacheSize = size; return this; }
        public Builder houseCacheSize(int size) { this.houseCacheSize = size; return this; }
        public Builder apartmentCacheSize(int size) { this.apartmentCacheSize = size; return this; }

        public EtlResult build() {
            return new EtlResult(this);
        }
    }
}
