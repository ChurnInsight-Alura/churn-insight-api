package com.alura.churnnsight.client;

import com.alura.churnnsight.config.FastApiProperties;
import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.dto.integration.DataIntegrationRequest;
import com.alura.churnnsight.dto.integration.DataIntegrationResponse;
import com.alura.churnnsight.exception.DownstreamException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class FastApiClient {
    private final WebClient webClient;
    private final FastApiProperties props;


    public FastApiClient(WebClient fastApiClient, FastApiProperties props) {
        this.webClient = fastApiClient;
        this.props = props;
    }
    public Mono<DataPredictionResult> predict(DataMakePrediction features) {
        Mono<DataPredictionResult> call = webClient.post()
                .uri(props.getPredictCustomerPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(features)
                .exchangeToMono(resp -> handleResponse(resp, DataPredictionResult.class))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        return mapNetworkErrors(call);
    }

    public Mono<DataIntegrationResponse> predictIntegration(DataIntegrationRequest request) {
        Mono<DataIntegrationResponse> call = webClient.post()
                .uri(props.getPredictCustomerPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchangeToMono(resp -> handleResponse(resp, DataIntegrationResponse.class))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        return mapNetworkErrors(call);

    }

    public Mono<List<DataIntegrationResponse>> predictBatch(List<DataIntegrationRequest> requests) {
        Mono<List<DataIntegrationResponse>> call = webClient.post()
                .uri(props.getPredictBatchPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requests)
                .exchangeToMono(resp -> handleResponse(resp, new ParameterizedTypeReference<List<DataIntegrationResponse>>() {}))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        return mapNetworkErrors(call);
    }

    public Mono<Map<String, Object>> predictBatchStats(List<DataIntegrationRequest> batch) {
        Mono<Map<String, Object>> call = webClient.post()
                .uri(props.getPredictBatchStatsPath()) // o desde properties si lo tienes configurable
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(batch)
                .exchangeToMono(resp -> handleResponse(resp, new ParameterizedTypeReference<Map<String, Object>>() {}));
        return mapNetworkErrors(call);
    }

    // Helpers

    private <T> Mono<T> handleResponse(ClientResponse resp, Class<T> type) {
        if (resp.statusCode().is2xxSuccessful()) {
            return resp.bodyToMono(type);
        }
        return resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(new DownstreamException(
                        "FASTAPI",
                        resp.statusCode().value(),
                        body
                )));
    }

    private <T> Mono<T> handleResponse(ClientResponse resp, ParameterizedTypeReference<T> type) {
        if (resp.statusCode().is2xxSuccessful()) {
            return resp.bodyToMono(type);
        }
        return resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(new DownstreamException(
                        "FASTAPI",
                        resp.statusCode().value(),
                        body
                )));
    }

    private <T> Mono<T> mapNetworkErrors(Mono<T> mono) {
        return mono
                .onErrorMap(WebClientRequestException.class,
                        ex -> new DownstreamException("FASTAPI", 502, ex.getMessage()))
                .onErrorMap(WebClientResponseException.class,
                        ex -> new DownstreamException("FASTAPI", ex.getStatusCode().value(), ex.getResponseBodyAsString()))
                .onErrorMap(TimeoutException.class,
                        ex -> new DownstreamException("FASTAPI", 504, "Timeout calling FASTAPI"));
    }

}
