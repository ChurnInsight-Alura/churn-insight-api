package com.alura.churnnsight.service;

import com.alura.churnnsight.client.FastApiClient;
import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PredictionService {
    private final FastApiClient fastApiClient;

    public PredictionService(FastApiClient fastApiClient) {
        this.fastApiClient = fastApiClient;
    }

    // Ya no devuelve null, ahora llama al cliente de Python
    public Mono<DataPredictionResult> predict(DataMakePrediction data) {
        return fastApiClient.predict(data);
    }
}