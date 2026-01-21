package com.alura.churnnsight.dto.creation;

import jakarta.validation.constraints.NotBlank;

public record DataAssignProduct(
        @NotBlank(message = "customerId no puede estar vacío")
        String customerId,
        @NotBlank(message = "productName no puede estar vacío")
        String productName
) {
}
