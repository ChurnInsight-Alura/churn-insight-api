package com.alura.churnnsight.dto.creation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record DataCreateCustomerTransaction(
        @NotBlank(message = "transactionId no puede estar vacío")
        String transactionId,
        @NotBlank(message = "El customerId no puede estar vacío")
        String customerId,
        @NotNull(message = "El transactionDate es requerido")
        LocalDateTime transactionDate,
        @PositiveOrZero(message = "El amount no puede ser negativo")
        Double amount,
        @NotBlank(message = "El transactionType es requerido")
        String transactionType
) {}
