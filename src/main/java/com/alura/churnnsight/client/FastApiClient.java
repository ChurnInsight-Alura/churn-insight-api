package com.alura.churnnsight.client;

import com.alura.churnnsight.config.FastApiProperties;
import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.dto.integration.DataIntegrationRequest;
import com.alura.churnnsight.dto.integration.DataIntegrationResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

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
                .uri(props.getPredictCustomerPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(features)
                .retrieve()
                .bodyToMono(DataPredictionResult.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()));
    }

    public Mono<DataIntegrationResponse> predictIntegration(DataIntegrationRequest request) {
        System.out.println("Enviando Body: " + request);
        return webClient.post()
                .uri(props.getPredictCustomerPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DataIntegrationResponse.class)
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()));
    }

    public Mono<List<DataIntegrationResponse>> predictBatch(List<DataIntegrationRequest> requests) {
        return webClient.post()
                .uri(props.getPredictBatchPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requests)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<DataIntegrationResponse>>() {})
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()));
    }

    public Mono<Map<String, Object>> predictBatchStats(List<DataIntegrationRequest> batch) {
        return webClient.post()
                .uri(props.getPredictBatchStatsPath()) // o desde properties si lo tienes configurable
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(batch)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
