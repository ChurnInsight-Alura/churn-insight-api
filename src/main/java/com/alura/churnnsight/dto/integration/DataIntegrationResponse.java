package com.alura.churnnsight.dto.integration;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataIntegrationResponse(
        @JsonProperty("CustomerId") String customerId,
        @JsonProperty("PredictedProba") Float predictedProba,
        @JsonProperty("PredictedLabel") Integer predictedLabel,
        @JsonProperty("CustomerSegment") String customerSegment,
        @JsonProperty("InterventionPriority") String interventionPriority,
        @JsonProperty("aiInsight") Object  aiInsight,
        @JsonProperty("aiInsightStatus") String aiInsightStatus

) {
}
