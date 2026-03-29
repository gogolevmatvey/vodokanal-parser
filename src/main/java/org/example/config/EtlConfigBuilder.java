package org.example.config;

import org.example.etl.EtlConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация для ETL-компонентов
 */
@Configuration
public class EtlConfigBuilder {

    @Bean
    public EtlConfig etlConfig(ConfigManager configManager) {
        return EtlConfig.builder()
                .inputFilePath(configManager.getFileInput())
                .validOutputPath(configManager.getFileOutputValid())
                .invalidOutputPath(configManager.getFileOutputInvalid())
                .correctedOutputPath(configManager.getFileOutputCorrected())
                .uncorrectedOutputPath(configManager.getFileOutputUncorrected())
                .errorReportPath(configManager.getFileOutputErrorReport())
                .batchSize(configManager.getEtlBatchSize())
                .progressInterval(configManager.getEtlProgressInterval())
                .build();
    }
}
