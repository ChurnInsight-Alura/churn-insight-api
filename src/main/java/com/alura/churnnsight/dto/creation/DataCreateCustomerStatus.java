package com.alura.churnnsight.dto.creation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DataCreateCustomerStatus(
        @NotBlank(message = "customerId no puede estar vacío")
        String customerId,
        @Min(value = 0, message = "El creditScore debe ser >= 0")
        Integer creditScore,
        Boolean isActiveMember,
        @Min(value = 0, message = "hasCrCard debe ser 0 o 1")
        Integer hasCrCard // 0/1
) {
}
