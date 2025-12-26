package com.alura.churnnsight.service;

import com.alura.churnnsight.client.FastApiClient;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.model.enumeration.Prevision;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PredictionService {

    private final FastApiClient fastApiClient;

    public PredictionService(FastApiClient fastApiClient) {
        this.fastApiClient = fastApiClient;
    }


    public Mono<DataPredictionResult> predictForClient(Long clientId) {

//        return Mono.fromCallable(() ->
//                userRepository.findById(clientId)
//                        .orElseThrow()
//        ).flatMap(user -> {
//
//            Map<String, Object> features = Map.of(
//                    "age", user.getAge(),
//                    "income", user.getIncome(),
//                    "tenure", user.getTenure()
//            );
//
//            return predictionClient.predict(features)
//                    .map(response ->
//                            new DataPredictionResult(
//                                    Prevision.fromPrediction(response.getPrediction()),
//                                    response.getPrediction()
//                            )
//                    );
//        });
//    }
        return null;
    }
}
