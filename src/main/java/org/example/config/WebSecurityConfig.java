package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Конфигурация безопасности и CORS
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final ConfigManager configManager;

    public WebSecurityConfig(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // REST API endpoints - требуют авторизации
                .requestMatchers("/api/localities/**").authenticated()
                .requestMatchers("/api/streets/**").authenticated()
                .requestMatchers("/api/houses/**").authenticated()
                .requestMatchers("/api/apartments/**").authenticated()
                .requestMatchers("/api/accounts/**").authenticated()
                .requestMatchers("/api/import/**").hasRole("ADMIN")
                .requestMatchers("/api/statistics").authenticated()
                // Статические ресурсы (Angular) - публичный доступ
                .requestMatchers("/", "/index.html", "/*.js", "/*.css", "/*.ico", "/assets/**").permitAll()
                // Страница логина
                .requestMatchers("/login").permitAll()
                // Все остальные запросы требуют авторизации
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/", true)
            )
            .logout(logout -> logout.permitAll());
        
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin"))
            .roles("ADMIN")
            .build();
        
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder.encode("user"))
            .roles("USER")
            .build();
        
        return new InMemoryUserDetailsManager(admin, user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(configManager.getCorsAllowedOrigins());
        configuration.setAllowedMethods(configManager.getCorsAllowedMethods());
        configuration.setAllowedHeaders(configManager.getCorsAllowedHeaders().equals("*") 
            ? java.util.Arrays.asList("*") 
            : java.util.Arrays.asList(configManager.getCorsAllowedHeaders().split("\\s*,\\s*")));
        configuration.setAllowCredentials(configManager.isCorsAllowCredentials());
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
