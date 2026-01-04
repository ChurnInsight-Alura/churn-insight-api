package com.alura.churnnsight.dto;

import com.alura.churnnsight.model.enumeration.Prevision;

public record DataPredictionResult(
       String CustomerId,
       Float PredictedProba,
       Integer PredictedLabel,
       String CustomerSegment,
       String InterventionPriority
) {

}
