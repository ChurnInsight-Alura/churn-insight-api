package com.alura.churnnsight.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "fastapi")

public class FastApiProperties {

    private String baseUrl;
    private String predictCustomerPath;
    private String predictBatchPath;
    private String predictBatchStatsPath;
    private int timeoutSeconds;
}
