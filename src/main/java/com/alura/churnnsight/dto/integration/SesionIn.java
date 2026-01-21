package com.alura.churnnsight.dto.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.alura.churnnsight.config.FlexibleLocalDateTimeDeserializer;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record SesionIn(
        @NotBlank(message = "El sessionId no puede estar vacío")
        @JsonProperty("SessionId") String sessionId,
        @JsonProperty("CustomerId") String customerId,
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
        @JsonProperty("SessionDate") LocalDateTime sessionDate,
        @JsonProperty("DurationMin") Float durationMin,
        @JsonProperty("UsedTransfer") Integer usedTransfer,
        @JsonProperty("UsedPayment") Integer usedPayment,
        @JsonProperty("UsedInvest") Integer usedInvest,
        @JsonProperty("OpenedPush") Integer openedPush,
        @JsonProperty("FailedLogin") Integer failedLogin
) {
}
