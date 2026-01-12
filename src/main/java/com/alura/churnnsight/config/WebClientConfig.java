package com.alura.churnnsight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {


    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    @Bean
    public WebClient fastApiWebClient(WebClient.Builder builder, FastApiProperties props) {
        return builder
                .baseUrl(props.getBaseUrl())
                .build();
    }
}