package org.example;

import org.example.config.ConfigManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Главное приложение Spring Boot
 */
@SpringBootApplication
public class VodokanalApplication {

    public static void main(String[] args) {
        SpringApplication.run(VodokanalApplication.class, args);
    }

    /**
     * Настройка CORS для доступа из браузера
     */
    @Bean
    public CorsFilter corsFilter() {
        ConfigManager config = ConfigManager.getInstance();

        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowCredentials(config.isCorsAllowCredentials());
        corsConfig.setAllowedOrigins(config.getCorsAllowedOrigins());
        corsConfig.setAllowedMethods(config.getCorsAllowedMethods());
        
        List<String> allowedHeaders = config.getCorsAllowedHeaders().equals("*") 
            ? List.of("*") 
            : List.of(config.getCorsAllowedHeaders().split("\\s*,\\s*"));
        corsConfig.setAllowedHeaders(allowedHeaders);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsFilter(source);
    }
}
