package com.alura.churnnsight.dto.consult;

import com.alura.churnnsight.model.Prediction;
import com.alura.churnnsight.model.enumeration.InterventionPriority;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.LocalDateTime;

public record DataPredictionDetail(
        Float predictedProba,
        Integer predictedLabel,
        String customerSegment,
        InterventionPriority interventionPriority,
        LocalDateTime predictedAt,
        @JsonRawValue String aiInsight,
        String aiInsightStatus

        ) {
    public DataPredictionDetail(Prediction prediction){
        this(
                (float) prediction.getPredictedProba(),
                prediction.getPredictedLabel(),
                prediction.getCustomerSegment(),
                prediction.getInterventionPriority(),
                prediction.getPredictedAt(),
                prediction.getAiInsight(),
                prediction.getAiInsightStatus()
        );
    }
}
