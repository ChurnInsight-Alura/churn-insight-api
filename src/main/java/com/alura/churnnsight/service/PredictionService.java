package com.alura.churnnsight.service;

import com.alura.churnnsight.dto.DataMakePrediction;
import com.alura.churnnsight.dto.DataPredictionResult;
import com.alura.churnnsight.model.enumeration.Prevision;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
public class PredictionService {

    public DataPredictionResult predict(DataMakePrediction data) {

        return new DataPredictionResult(Prevision.fromPrediction(.5),.5);
    }
}
