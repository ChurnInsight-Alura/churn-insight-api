package com.alura.churnnsight.controller;

import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.service.PredictionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/predict")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @GetMapping
    public String prediction(){
            return  "Deposite microservicio";
        }
    @PostMapping
    public ResponseEntity inferPrediction(@RequestBody @Valid DataMakePrediction data){
        var dataPredictionResult = predictionService.predict(data);
        return  ResponseEntity.ok(dataPredictionResult);
        }
}
