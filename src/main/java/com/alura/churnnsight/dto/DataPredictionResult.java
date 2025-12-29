package com.alura.churnnsight.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataPredictionResult(
        @JsonProperty("CustomerId")
        String customerId,

        @JsonProperty("PredictedProba")
        Double predictedProba,

        @JsonProperty("PredictedLabel")
        Integer predictedLabel,

        @JsonProperty("CustomerSegment")
        String customerSegment,

        @JsonProperty("InterventionPriority")
        String interventionPriority
) {}