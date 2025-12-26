package com.alura.churnnsight.client;

import com.alura.churnnsight.config.FastApiProperties;
import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class FastApiClient {
    private final WebClient webClient;
    private final FastApiProperties props;


    public FastApiClient(WebClient fastApiClient, FastApiProperties props) {
        this.webClient = fastApiClient;
        this.props = props;
    }
    public Mono<DataPredictionResult> predict(DataMakePrediction features) {
        return webClient.post()
                .uri(props.getPredictPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(features)
                .retrieve()
                .bodyToMono(DataPredictionResult.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()));
    }
}
