package org.example;

import org.example.config.ConfigManager;
import org.example.etl.EtlResult;
import org.example.etl.EtlService;
import org.example.etl.ReportGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.example.config.DatabaseConfig;
import org.example.config.EtlConfigBuilder;

/**
 * Точка входа приложения.
 * Запускает ETL-обработку данных водоканал.
 */
public class Main {

    public static void main(String[] args) {
        // Создаём Spring ApplicationContext
        ApplicationContext context = new AnnotationConfigApplicationContext(
                DatabaseConfig.class,
                EtlConfigBuilder.class
        );

        // Получаем ETL сервис из Spring DI
        EtlService etlService = context.getBean(EtlService.class);

        System.out.println("Запуск ETL-обработки...");

        // Выполняем обработку
        EtlResult result = etlService.process();

        // Вывод статистики
        System.out.println();
        System.out.println("=== Результаты обработки ===");
        System.out.println("Всего обработано записей: " + result.getTotalCount());
        System.out.printf("Успешно обработано: %d (%.2f%%)%n", result.getValidCount(), result.getValidPercent());
        System.out.printf("С ошибками: %d (%.2f%%)%n", result.getInvalidCount(), result.getInvalidPercent());
        System.out.printf("Исправлено записей: %d (%.2f%% от ошибок)%n", result.getCorrectedCount(), result.getCorrectedPercent());
        System.out.printf("Не исправлено записей: %d (%.2f%% от ошибок)%n", result.getUncorrectedCount(), result.getUncorrectedPercent());
        System.out.println();

        ConfigManager config = context.getBean(ConfigManager.class);
        System.out.println("Валидные записи сохранены в файл: " + config.getFileOutputValid());
        System.out.println("Записи с ошибками сохранены в файл: " + config.getFileOutputInvalid());
        System.out.println("Исправленные записи сохранены в файл: " + config.getFileOutputCorrected());
        System.out.println("Неисправленные ошибки сохранены в файл: " + config.getFileOutputUncorrected());
        System.out.println();

        System.out.println("=== Статистика базы данных ===");
        System.out.println("Записей сохранено в базу данных: " + result.getDbSavedCount());
        System.out.println("Ошибок при сохранении в БД: " + result.getDbErrorCount());
        System.out.println();
        System.out.println("Баланс: " + result.getTotalCount() + " (всего) - " + result.getUncorrectedCount() + " (не исправлено) - " + result.getDbErrorCount() + " (ошибки БД) = " + (result.getTotalCount() - result.getUncorrectedCount() - result.getDbErrorCount()));

        // Вывод детальной статистики по ошибкам БД
        if (!result.getDbErrorCounts().isEmpty()) {
            System.out.println();
            System.out.println("=== Детальная статистика ошибок БД ===");
            System.out.println("Всего типов ошибок: " + result.getDbErrorCounts().size());
            System.out.println();

            result.getDbErrorCounts().entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    String errorType = entry.getKey();
                    int count = entry.getValue();
                    double percent = result.getDbErrorCount() > 0 ? (double) count / result.getDbErrorCount() * 100 : 0;
                    System.out.printf("  %s: %d (%.2f%%)%n", errorType, count, percent);
                });
        }

        // Вывод метрик производительности
        ReportGenerator reportGenerator = context.getBean(ReportGenerator.class);
        System.out.print(reportGenerator.formatPerformanceMetrics(result));

        // Закрываем контекст (и пул соединений)
        ((AnnotationConfigApplicationContext) context).close();
    }
}
