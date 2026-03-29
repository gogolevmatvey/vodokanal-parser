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

        public EtlResult build() {
            return new EtlResult(this);
        }
    }
}
