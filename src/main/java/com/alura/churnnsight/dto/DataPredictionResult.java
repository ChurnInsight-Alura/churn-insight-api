package com.alura.churnnsight.dto;

public record DataPredictionResult(
       String CustomerId,
       Float PredictedProba,
       Integer PredictedLabel,
       String CustomerSegment,
       String InterventionPriority
) {

}
