package com.alura.churnnsight.dto.creation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record DataCreateCustomer(
        @NotBlank(message = "customerId no puede estar vacío")
        String customerId,
        @NotBlank(message = "Geography es requerido")
        String geography,
        @NotBlank(message = "Gender es requerido")
        String gender,
        @NotNull(message = "El birthDate es requerido")
        LocalDate birthDate,
        LocalDate createdAt,
        String surname,
        @PositiveOrZero(message = "EstimatedSalary no puede ser negativo")
        Double estimatedSalary,
        String customerSegment
) {
}
