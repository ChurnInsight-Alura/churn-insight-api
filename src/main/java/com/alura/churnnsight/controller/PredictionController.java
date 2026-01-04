package com.alura.churnnsight.controller;

import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/predict")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @GetMapping
    public String prediction(){
            return  "Deposite microservicio";
        }

    @PostMapping("/{customerId}")
    public Mono<ResponseEntity<DataPredictionResult>> inferPrediction(@PathVariable String customerId) {
        return predictionService.predictForClient(customerId)
                .map(ResponseEntity::ok);
    }

}
