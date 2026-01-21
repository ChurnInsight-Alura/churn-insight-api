package com.alura.churnnsight.dto.creation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record DataCreateCustomerSession(
        @NotBlank(message = "sessionId no puede estar vacío")
        String sessionId,
        @NotBlank(message = "El customerId no puede estar vacío")
        String customerId,
        @NotNull(message = "El sessionDate es requerido")
        LocalDateTime sessionDate,
        @PositiveOrZero(message = "La durationMin no puede ser negativo")
        Double durationMin,
        @Min(value = 0, message = "El usedTransfer no puede ser negativo")
        Integer usedTransfer,
        @Min(value = 0, message = "El usedPayment no puede ser negativo")
        Integer usedPayment,
        @Min(value = 0, message = "El usedInvest no puede ser negativo")
        Integer usedInvest,
        @Min(value = 0, message = "openedPush no puede ser negativo")
        Integer openedPush,
        @Min(value = 0, message = "failedLogin no puede ser negativo")
        Integer failedLogin
) {}
