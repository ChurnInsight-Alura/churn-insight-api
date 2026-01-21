package com.alura.churnnsight.dto.creation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record DataCreateAccount(
        @NotBlank(message = "customerId no puede estar vacío")
        String customerId,
        @PositiveOrZero(message = "balance no puede ser negativo")
        Double balance
){
        }
