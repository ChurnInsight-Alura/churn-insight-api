package com.alura.churnnsight.dto.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.alura.churnnsight.config.FlexibleLocalDateTimeDeserializer;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record TransaccionIn(
        @NotBlank(message = "El transactionId no puede estar vacío")
        @JsonProperty("TransactionId") String transactionId,
        @JsonProperty("CustomerId") String customerId,
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        @JsonProperty("TransactionDate") LocalDateTime transactionDate,
        @JsonProperty("Amount") Float amount,
        @JsonProperty("TransactionType") String transactionType
) {
}
