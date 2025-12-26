package com.alura.churnnsight.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "fastapi")
@Getter
@Setter
public class FastApiProperties {

    private String baseUrl;
    private String predictPath;
    private int timeoutSeconds;
}
