package org.example.etl;

/**
 * Конфигурация ETL-процесса
 */
public class EtlConfig {

    private final String inputFilePath;
    private final String validOutputPath;
    private final String invalidOutputPath;
    private final String correctedOutputPath;
    private final String uncorrectedOutputPath;
    private final String errorReportPath;
    private final int batchSize;
    private final int progressInterval;

    private EtlConfig(Builder builder) {
        this.inputFilePath = builder.inputFilePath;
        this.validOutputPath = builder.validOutputPath;
        this.invalidOutputPath = builder.invalidOutputPath;
        this.correctedOutputPath = builder.correctedOutputPath;
        this.uncorrectedOutputPath = builder.uncorrectedOutputPath;
        this.errorReportPath = builder.errorReportPath;
        this.batchSize = builder.batchSize;
        this.progressInterval = builder.progressInterval;
    }

    public String getInputFilePath() { return inputFilePath; }
    public String getValidOutputPath() { return validOutputPath; }
    public String getInvalidOutputPath() { return invalidOutputPath; }
    public String getCorrectedOutputPath() { return correctedOutputPath; }
    public String getUncorrectedOutputPath() { return uncorrectedOutputPath; }
    public String getErrorReportPath() { return errorReportPath; }
    public int getBatchSize() { return batchSize; }
    public int getProgressInterval() { return progressInterval; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String inputFilePath;
        private String validOutputPath;
        private String invalidOutputPath;
        private String correctedOutputPath;
        private String uncorrectedOutputPath;
        private String errorReportPath;
        private int batchSize = 500;
        private int progressInterval = 1000;

        public Builder inputFilePath(String path) { this.inputFilePath = path; return this; }
        public Builder validOutputPath(String path) { this.validOutputPath = path; return this; }
        public Builder invalidOutputPath(String path) { this.invalidOutputPath = path; return this; }
        public Builder correctedOutputPath(String path) { this.correctedOutputPath = path; return this; }
        public Builder uncorrectedOutputPath(String path) { this.uncorrectedOutputPath = path; return this; }
        public Builder errorReportPath(String path) { this.errorReportPath = path; return this; }
        public Builder batchSize(int size) { this.batchSize = size; return this; }
        public Builder progressInterval(int interval) { this.progressInterval = interval; return this; }

        public EtlConfig build() {
            return new EtlConfig(this);
        }
    }
}
