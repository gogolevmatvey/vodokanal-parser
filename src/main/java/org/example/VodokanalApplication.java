package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot приложение для веб-интерфейса
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "org.example.controller",
    "org.example.service",
    "org.example.config"
})
public class VodokanalApplication {

    public static void main(String[] args) {
        SpringApplication.run(VodokanalApplication.class, args);
    }
}
