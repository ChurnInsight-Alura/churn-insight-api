package com.alura.churnnsight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        // Si no hay CORS definido, NO registramos nada
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return;
        }

        String[] origins = allowedOrigins.split("\\s*,\\s*");

        registry.addMapping("/**")
                // ORIGINS del front
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .exposedHeaders("Authorization");
        // .allowCredentials(true)  //
    }
}
